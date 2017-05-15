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

import org.eclipse.che.api.project.shared.dto.event.FileTrackingOperationDto;

/**
 * Describes file tracking operation call from client. There are several types of such calls:
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
 * @author Roman Nikitenko
 */
public class FileTrackingOperationEvent {
    private final String endpointId;
    private final FileTrackingOperationDto fileTrackingOperation;

    FileTrackingOperationEvent(String endpointId, FileTrackingOperationDto fileTrackingOperation) {
        this.endpointId = endpointId;
        this.fileTrackingOperation = fileTrackingOperation;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public FileTrackingOperationDto getFileTrackingOperation() {
        return fileTrackingOperation;
    }
}
