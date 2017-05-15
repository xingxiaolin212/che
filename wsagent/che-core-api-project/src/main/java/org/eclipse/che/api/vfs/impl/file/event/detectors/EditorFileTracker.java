/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.vfs.impl.file.event.detectors;

import com.google.common.hash.Hashing;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.jsonrpc.RequestTransmitter;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.project.shared.dto.event.FileStateUpdateDto;
import org.eclipse.che.api.project.shared.dto.event.FileTrackingOperationDto;
import org.eclipse.che.api.project.shared.dto.event.FileTrackingOperationDto.Type;
import org.eclipse.che.api.vfs.Path;
import org.eclipse.che.api.vfs.VirtualFile;
import org.eclipse.che.api.vfs.VirtualFileSystemProvider;
import org.eclipse.che.api.vfs.watcher.FileWatcherManager;
import org.eclipse.che.api.vfs.watcher.FileWatcherUtils;
import org.slf4j.Logger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static java.nio.charset.Charset.defaultCharset;
import static org.eclipse.che.api.project.shared.dto.event.FileWatcherEventType.DELETED;
import static org.eclipse.che.api.project.shared.dto.event.FileWatcherEventType.MODIFIED;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Receive a file tracking operation call from client. There are several type of such calls:
 * <ul>
 *     <li>
 *         START/STOP - tells to start/stop tracking specific file
 *     </li>
 *     <li>
 *         SUSPEND/RESUME - tells to start/stop tracking all files registered for specific endpoint
 *     </li>
 *     <li>
 *         MOVE - tells that file that is being tracked should be moved (renamed)
 *     </li>
 * </ul>
 *
 * @author Dmitry Kuleshov
 */
@Singleton
public class EditorFileTracker {
    private static final Logger LOG = getLogger(EditorFileTracker.class);

    private static final String OUTGOING_METHOD = "event:file-state-changed";

    private final Map<String, String>  hashRegistry    = new HashMap<>();
    private final Map<String, Integer> watchIdRegistry = new HashMap<>();

    private final RequestTransmitter                          transmitter;
    private       File                                        root;
    private final FileWatcherManager                          fileWatcherManager;
    private final VirtualFileSystemProvider                   vfsProvider;
    private final EventService                                eventService;
    private final EventSubscriber<FileTrackingOperationEvent> fileOperationEventSubscriber;


    @Inject
    public EditorFileTracker(@Named("che.user.workspaces.storage") File root,
                             FileWatcherManager fileWatcherManager,
                             RequestTransmitter transmitter,
                             VirtualFileSystemProvider vfsProvider,
                             EventService eventService) {
        this.root = root;
        this.fileWatcherManager = fileWatcherManager;
        this.transmitter = transmitter;
        this.vfsProvider = vfsProvider;
        this.eventService = eventService;

        fileOperationEventSubscriber = new EventSubscriber<FileTrackingOperationEvent>() {
            @Override
            public void onEvent(FileTrackingOperationEvent event) {
                onFileTrackingOperationReceived(event.getEndpointId(), event.getFileTrackingOperation());
            }
        };
        eventService.subscribe(fileOperationEventSubscriber);
    }

    private void onFileTrackingOperationReceived(String endpointId, FileTrackingOperationDto operation) {
        Type type = operation.getType();
        String path = operation.getPath();
        String oldPath = operation.getOldPath();

        switch (type) {
            case START: {
                String key = path + endpointId;
                LOG.debug("Received file tracking operation START trigger key : {}", key);
                if (watchIdRegistry.containsKey(key)) {
                    LOG.debug("Already registered {}", key);
                    return;
                }
                int id = fileWatcherManager.registerByPath(path,
                                                           getCreateConsumer(endpointId, path),
                                                           getModifyConsumer(endpointId, path),
                                                           getDeleteConsumer(endpointId, path));
                watchIdRegistry.put(key, id);

                break;
            }
            case STOP: {
                LOG.debug("Received file tracking operation STOP trigger.");

                Integer id = watchIdRegistry.remove(path + endpointId);
                if (id != null) {
                    fileWatcherManager.unRegisterByPath(id);
                }

                break;
            }
            case SUSPEND: {
                LOG.debug("Received file tracking operation SUSPEND trigger.");

                fileWatcherManager.suspend();

                break;
            }
            case RESUME: {
                LOG.debug("Received file tracking operation RESUME trigger.");

                fileWatcherManager.resume();

                break;
            }
            case MOVE: {
                LOG.debug("Received file tracking operation MOVE trigger.");


                Integer oldId = watchIdRegistry.remove(oldPath + endpointId);
                if (oldId != null) {
                    fileWatcherManager.unRegisterByPath(oldId);
                }

                int newId = fileWatcherManager.registerByPath(path,
                                                              getCreateConsumer(endpointId, path),
                                                              getModifyConsumer(endpointId, path),
                                                              getDeleteConsumer(endpointId, path));
                watchIdRegistry.put(path + endpointId, newId);

                break;
            }
            default: {
                LOG.error("Received file tracking operation UNKNOWN trigger.");

                break;
            }
        }
    }

    private Consumer<String> getCreateConsumer(String endpointId, String path) {
        // for case when file is updated through recreation
        return getModifyConsumer(endpointId, path);
    }

    private Consumer<String> getModifyConsumer(String endpointId, String path) {
        return it -> {
            String newHash = hashFile(path);
            String oldHash = hashRegistry.getOrDefault(path + endpointId, null);

            if (Objects.equals(newHash, oldHash)) {
                return;
            }

            hashRegistry.put(path + endpointId, newHash);

            FileStateUpdateDto params = newDto(FileStateUpdateDto.class).withPath(path).withType(MODIFIED).withHashCode(newHash);
            transmitter.transmitOneToNone(endpointId, OUTGOING_METHOD, params);
        };
    }

    private Consumer<String> getDeleteConsumer(String endpointId, String path) {
        return it -> new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!Files.exists(FileWatcherUtils.toNormalPath(root.toPath(), it))) {
                    FileStateUpdateDto params = newDto(FileStateUpdateDto.class).withPath(path).withType(DELETED);
                    transmitter.transmitOneToNone(endpointId, OUTGOING_METHOD, params);
                }

            }
        }, 1_000L);
    }

    private String hashFile(String path) {
        try {
            VirtualFile file = vfsProvider.getVirtualFileSystem().getRoot().getChild(Path.of(path));
            return Hashing.md5().hashString(file == null ? "" : file.getContentAsString(), defaultCharset()).toString();
        } catch (ServerException | ForbiddenException e) {
            LOG.error("Error trying to read {} file and broadcast it", path, e);
        }
        return null;
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(fileOperationEventSubscriber);
    }
}
