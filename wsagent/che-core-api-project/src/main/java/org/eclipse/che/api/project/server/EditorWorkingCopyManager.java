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
package org.eclipse.che.api.project.server;

import com.google.common.hash.Hashing;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.project.server.notification.EditorContentUpdatedEvent;
import org.eclipse.che.api.project.shared.dto.EditorChangesDto;
import org.eclipse.che.api.project.shared.dto.event.FileTrackingOperationDto;
import org.eclipse.che.api.vfs.impl.file.event.detectors.FileTrackingOperationEvent;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class EditorWorkingCopyManager {
    private static final String WORKING_COPIES_DIR = "/" + CHE_DIR + "/workingCopies";

    private Provider<ProjectManager>                    projectManagerProvider;
    private EventService                                eventService;
    private EventSubscriber<FileTrackingOperationEvent> fileOperationEventSubscriber;

    private final Map<String, EditorWorkingCopy> workingCopiesStorage = new HashMap<>();

    @Inject
    public EditorWorkingCopyManager(Provider<ProjectManager> projectManagerProvider,
                                    EventService eventService) {
        this.projectManagerProvider = projectManagerProvider;
        this.eventService = eventService;

        fileOperationEventSubscriber = new EventSubscriber<FileTrackingOperationEvent>() {
            @Override
            public void onEvent(FileTrackingOperationEvent event) {
                onFileOperation(event.getFileTrackingOperation());
            }
        };
        eventService.subscribe(fileOperationEventSubscriber);
    }

    public File getWorkingCopyFile(String filePath) throws NotFoundException, ServerException {
        String projectPath = projectManagerProvider.get().asFile(filePath).getProject();
        VirtualFileEntry virtualFileEntry = getPersistentWorkingCopy(filePath, projectPath);
        return virtualFileEntry == null ? null : new File(virtualFileEntry.getVirtualFile().toIoFile().getAbsolutePath());
    }

    public String getContentFor(String filePath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        EditorWorkingCopy workingCopy = workingCopiesStorage.get(filePath);
        if (workingCopy != null) {
            return workingCopy.getContentAsString();
        }

        FileEntry originalFile = projectManagerProvider.get().asFile(filePath);
        if (originalFile == null) {
            throw new NotFoundException(format("Item '%s' isn't found. ", filePath));
        }

        String projectPath = originalFile.getProject();
        VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(filePath, projectPath);
        return persistentWorkingCopy == null ? originalFile.getVirtualFile().getContentAsString()
                                             : persistentWorkingCopy.getVirtualFile().getContentAsString();
    }

    void onEditorContentUpdated(String endpointId, EditorChangesDto changes) {
        try {
            String filePath = changes.getFileLocation();
            String projectPath = changes.getProjectPath();
            String text = changes.getText();
            int offset = changes.getOffset();
            int removedCharCount = changes.getRemovedCharCount();
            EditorWorkingCopy workingCopy = workingCopiesStorage.get(filePath);
            if (workingCopy == null) {
                workingCopy = createWorkingCopy(filePath);
            }

            String newContent = null;
            String oldContent = workingCopy.getContentAsString();
            System.out.println("*********** EditorWorkingCopyManager onWorkingCopyChanged " + changes.getType());
            switch (changes.getType()) {
                case INSERT: {
                    newContent = new StringBuilder(oldContent).insert(offset, text).toString();
                    break;
                }
                case REMOVE: {
                    if (removedCharCount > 0) {
                        newContent = new StringBuilder(oldContent).delete(offset, offset + removedCharCount).toString();
                    }
                    break;
                }
                case REPLACE_ALL: {
                    newContent = new StringBuilder(oldContent).replace(0, oldContent.length(), text).toString();
                    break;
                }
                default: {
                    break;
                }

            }

            if (newContent != null) {
                //TODO
                System.out.println("*********** EditorWorkingCopyManager onWorkingCopyChanged update workingCopy " + filePath);

                workingCopy.updateContent(newContent);
                VirtualFileEntry virtualFileEntry = getPersistentWorkingCopy(filePath, projectPath);
                virtualFileEntry.getVirtualFile().updateContent(newContent);
                eventService.publish(new EditorContentUpdatedEvent(endpointId, changes));
            }

        } catch (Exception e) {
            //TODO handle exception
            System.out.println(e.getMessage());
        }
    }


    private void onFileOperation(FileTrackingOperationDto operation) {
        try {
            FileTrackingOperationDto.Type type = operation.getType();
            System.out.println("*********** EditorWorkingCopyManager onFileOperation " + type);
            switch (type) {
                case START: {
                    String path = operation.getPath();
                    EditorWorkingCopy workingCopy = workingCopiesStorage.get(path);
                    if (workingCopy == null) {
                        createWorkingCopy(path);
                    }

                    // At opening file we can have persistent working copy ONLY when user has unsaved data
                    // TODO We need provide ability to compare and save this unsaved data
                    break;
                }
                case STOP: {
                    String path = operation.getPath();
                    if (isWorkingCopyHasUnsavedData(path)) {
                        //at this case we do not remove persistent working copy to have ability to recover unsaved data
                        // when the file will be open later
                        return;
                    }

                    VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(path, operation.getProjectPath());
                    if (persistentWorkingCopy != null) {
                        persistentWorkingCopy.remove();
                    }
                    break;
                }

                case MOVE: {
                    String oldPath = operation.getOldPath();
                    String newPath = operation.getPath();
                    if (isNullOrEmpty(oldPath) || isNullOrEmpty(newPath)) {
                        //TODO
                        return;
                    }

                    EditorWorkingCopy workingCopy = workingCopiesStorage.remove(oldPath);
                    if (workingCopy == null) {
                        return;
                    }

                    String workingCopyNewPath = getWorkingCopyPath(newPath);
                    workingCopy.setPath(workingCopyNewPath);
                    workingCopiesStorage.put(newPath, workingCopy);

                    String projectPath = workingCopy.getProjectPath();
                    VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(oldPath, projectPath);
                    if (persistentWorkingCopy != null) {
                        persistentWorkingCopy.remove();
                    }

                    FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
                    if (persistentWorkingCopiesStorage == null) {
                        persistentWorkingCopiesStorage = createWorkingCopiesStorage(projectPath);
                    }

                    persistentWorkingCopiesStorage.createFile(workingCopyNewPath, workingCopy.getContentAsBytes());
                    break;
                }

                default: {
                    break;
                }
            }

        } catch (NotFoundException | ServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ForbiddenException e) {
            e.printStackTrace();
        } catch (ConflictException e) {
            e.printStackTrace();
        }
    }

    private boolean isWorkingCopyHasUnsavedData(String path) {
        try {
            EditorWorkingCopy workingCopy = workingCopiesStorage.remove(path);
            FileEntry originalFile = originalFile = projectManagerProvider.get().asFile(path);
            if (workingCopy == null || originalFile == null) {
                return false;
            }

            String workingCopyContent = workingCopy.getContentAsString();
            String originalFileContent = originalFile.getVirtualFile().getContentAsString();
            if (workingCopyContent == null || originalFileContent == null) {
                return false;
            }

            String workingCopyHash = Hashing.md5().hashString(workingCopyContent, defaultCharset()).toString();
            String originalFileHash = Hashing.md5().hashString(originalFileContent, defaultCharset()).toString();
            if (!Objects.equals(workingCopyHash, originalFileHash)) {
                return true;
            }
        } catch (NotFoundException | ServerException | ForbiddenException e) {
            e.printStackTrace();
        }
        return false;
    }

    private EditorWorkingCopy createWorkingCopy(String filePath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException, IOException {

        FileEntry file = projectManagerProvider.get().asFile(filePath);
        if (file == null) {
            //TODO
            return null;
        }

        String projectPath = file.getProject();
        FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
        if (persistentWorkingCopiesStorage == null) {
            persistentWorkingCopiesStorage = createWorkingCopiesStorage(projectPath);
        }


        String workingCopyPath = getWorkingCopyPath(filePath);
        byte[] content = file.contentAsBytes();
        persistentWorkingCopiesStorage.createFile(workingCopyPath, content);

        EditorWorkingCopy workingCopy = new EditorWorkingCopy(workingCopyPath, projectPath, file.contentAsBytes());
        workingCopiesStorage.put(filePath, workingCopy);
        return workingCopy;
    }

    private VirtualFileEntry getPersistentWorkingCopy(String filePath, String projectPath) {
        VirtualFileEntry entry = null;
        try {
            FolderEntry workingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
            if (workingCopiesStorage == null) {
                return null;
            }

            String workingCopyPath = getWorkingCopyPath(filePath);
            return workingCopiesStorage.getChild(workingCopyPath);
        } catch (ServerException e) {
            //ignore
        }
        return entry;
    }

    private FolderEntry getPersistentWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManagerProvider.get().getProject(projectPath);
            FolderEntry baseFolder = project.getBaseFolder();
            if (baseFolder == null) {
                return null;
            }

            String tempDirectoryPath = baseFolder.getPath().toString() + WORKING_COPIES_DIR;
            return projectManagerProvider.get().asFolder(tempDirectoryPath);
        } catch (Exception e) {
            return null;
        }
    }

    private FolderEntry createWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManagerProvider.get().getProject(projectPath);
            FolderEntry baseFolder = project.getBaseFolder();
            if (baseFolder == null) {
                return null;
            }

            return baseFolder.createFolder(WORKING_COPIES_DIR);
        } catch (Exception e) {
            return null;
        }
    }

    private String getWorkingCopyPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return path.replace('/', '.');
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(fileOperationEventSubscriber);
    }
}
