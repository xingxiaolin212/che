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
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

/**
 * @author Roman Nikitenko
 */
public class EditorWorkingCopy {

    private String path;
    private String projectPath;
    private byte[]            content;

    public EditorWorkingCopy(String path, String projectPath, byte[] content) {
        this.path = path;
        this.projectPath = projectPath;
        this.content = content;
    }

    public byte[] getContentAsBytes() {
        return content;
    }

    public String getContentAsString() {
        return new String(content);
    }

    public EditorWorkingCopy updateContent(byte[] content) {
        this.content = content;
        return this;
    }

    public EditorWorkingCopy updateContent(String content) {
        this.content = content.getBytes();
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getProjectPath() {
        return projectPath;
    }
}
