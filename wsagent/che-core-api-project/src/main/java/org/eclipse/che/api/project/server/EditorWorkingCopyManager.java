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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class EditorWorkingCopyManager {
    private static final String WORKING_COPIES_DIR = "/" + CHE_DIR + "/workingCopies";
//    private static final String OUTGOING_METHOD    = "track:editor-content";

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
                    if (workingCopy != null) {
                        System.out.println("*********** EditorWorkingCopyManager working copy already exist !!! ");
                        //TODO check hashes of contents for working copy and base file
                    } else {
                        System.out.println("*********** EditorWorkingCopyManager CREATE working copy ");
                        createWorkingCopy(path);
                    }

                    break;
                }
                case RESUME: {
//                    requestUpdateContent();
                    break;
                }
                case STOP: {
                    String path = operation.getPath();
                    //TODO check hashes of contents for working copy and base file and remove persitent copy
                    EditorWorkingCopy workingCopy = workingCopiesStorage.remove(path);

                    VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(path, operation.getProjectPath());
                    if (workingCopy != null) {
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
                    if (workingCopy != null) {
                        String newWorkingCopyPath = getWorkingCopyFileName(newPath);
                        workingCopy.setPath(newWorkingCopyPath);
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

                        persistentWorkingCopiesStorage.createFile(newWorkingCopyPath, workingCopy.getContentAsBytes());

                    }
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

//    private void requestUpdateContent() {
//
//    }

    private EditorWorkingCopy createWorkingCopy(String filePath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException, IOException {

        //TODO get projectPath from file, remove parametr
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


        String workingCopyPath = getWorkingCopyFileName(filePath);
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

            String workingCopyPath = getWorkingCopyFileName(filePath);
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

    private String getWorkingCopyFileName(String path) {
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
