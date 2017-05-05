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
import org.eclipse.che.api.vfs.Path;
import org.eclipse.che.api.vfs.impl.file.event.detectors.FileTrackingOperationEvent;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

import static java.lang.String.format;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class EditorWorkingCopyManager {
    private static final String WORKING_COPIES_DIR = "/" + CHE_DIR + "/workingCopies";

    private ProjectManager projectManager;
    private EventService   eventService;
    EventSubscriber<FileTrackingOperationEvent> fileOperationEventSubscriber;

    @Inject
    public EditorWorkingCopyManager(ProjectManager projectManager,
                                    EventService eventService) {
        this.projectManager = projectManager;
        this.eventService = eventService;

        fileOperationEventSubscriber = new EventSubscriber<FileTrackingOperationEvent>() {
            @Override
            public void onEvent(FileTrackingOperationEvent event) {
                onFileOperation(event.getFileTrackingOperation());
            }
        };
        eventService.subscribe(fileOperationEventSubscriber);
    }

    public File getWorkingCopyFile(String filePath) {
            VirtualFileEntry virtualFileEntry = getWorkingCopy(filePath);
            return virtualFileEntry == null ? null : new File(virtualFileEntry.getVirtualFile().toIoFile().getAbsolutePath());
    }

    public String getWorkingCopyContent(String filePath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {

        VirtualFileEntry virtualFileEntry = getWorkingCopy(filePath);
        return virtualFileEntry == null ? null : virtualFileEntry.getVirtualFile().getContentAsString();
    }

    void onEditorContentUpdated(String endpointId, EditorChangesDto changes) {
        try {
            //TODO handle rename file
            String filePath = changes.getFileLocation();
            String projectPath = changes.getProjectPath();
            String text = changes.getText();
            int offset = changes.getOffset();
            int removedCharCount = changes.getRemovedCharCount();
            VirtualFileEntry workingCopy = getWorkingCopy(filePath);
            if (workingCopy == null) {
                workingCopy = createWorkingCopy(filePath, projectPath);
            }

            String newContent = null;
            String oldContent = workingCopy.getVirtualFile().getContentAsString();
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
                System.out.println("*********** EditorWorkingCopyManager onWorkingCopyChanged update workingCopy ");

                workingCopy.getVirtualFile().updateContent(newContent);
                eventService.publish(new EditorContentUpdatedEvent(endpointId, oldContent, changes, newContent));
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
                    String projectPath = projectManager.asFile(path).getProject();
                    VirtualFileEntry workingCopy = getWorkingCopy(path);
                    if (workingCopy != null) {
                        System.out.println("*********** EditorWorkingCopyManager working copy already exist !!! ");
                        //TODO check hashes of contents for working copy and base file
                    } else {
                        System.out.println("*********** EditorWorkingCopyManager CREATE working copy ");
                        createWorkingCopy(path, projectPath);
                    }
                    break;
                }
                case STOP: {
                    String path = operation.getPath();
                    //TODO check hashes of contents for working copy and base file
                    VirtualFileEntry workingCopy = getWorkingCopy(path);
                    if (workingCopy != null) {
                        workingCopy.remove();
                    }
                    break;
                }
                case SUSPEND: {


                    break;
                }
                case RESUME: {

                    break;
                }
                case MOVE: {
                    String oldPath = operation.getOldPath();
                    if (oldPath == null) {
                        return;
                    }

                    String path = operation.getPath();
                    VirtualFileEntry workingCopy = getWorkingCopy(oldPath);
                    String newName = getWorkingCopyFileName(path);
                    if (workingCopy != null) {
                        workingCopy.getVirtualFile().rename(newName);
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

    private VirtualFileEntry createWorkingCopy(String filePath, String projectPath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException, IOException {
        FileEntry file = projectManager.asFile(filePath);//TODO when file null???

        String workingCopyPath = getWorkingCopyFileName(filePath);
        FolderEntry workingCopiesStorage = getWorkingCopiesStorage(projectPath);
        if (workingCopiesStorage == null) {
            workingCopiesStorage = createWorkingCopiesStorage(projectPath);
        }

        return workingCopiesStorage.createFile(workingCopyPath, file.getInputStream());
    }

    private VirtualFileEntry getWorkingCopy(String filePath) {
        VirtualFileEntry entry = null;
        try {
            entry = projectManager.getProjectsRoot().getChild(filePath);
            if (entry == null) {
                return null;
            }

            String projectPath = entry.getProject();
            FolderEntry workingCopiesStorage = getWorkingCopiesStorage(projectPath);
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

    private FolderEntry getWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManager.getProject(projectPath);
            FolderEntry baseFolder = project.getBaseFolder();
            if (baseFolder == null) {
                return null;
            }

            String tempDirectoryPath = baseFolder.getPath().toString() + WORKING_COPIES_DIR;
            return projectManager.asFolder(tempDirectoryPath);
        } catch (Exception e) {
            return null;
        }
    }

    private FolderEntry createWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManager.getProject(projectPath);
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
