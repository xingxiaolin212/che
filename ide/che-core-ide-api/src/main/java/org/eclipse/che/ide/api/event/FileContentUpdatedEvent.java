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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that notifies that editor content was updated.
 */
public class FileContentUpdatedEvent extends GwtEvent<FileContentUpdatedEvent.FileContentUpdatedHandler> {
    /** The event type. */
    public static Type<FileContentUpdatedHandler> TYPE = new Type<>();

    /**
     * The path to the file that is updated.
     */
    private final String filePath;


    /**
     * Creates new {@link org.eclipse.che.ide.api.event.FileContentUpdatedEvent}.
     *
     * @param filePath
     *         the path of the file that is updated.
     */
    public FileContentUpdatedEvent(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * Returns the path to the file that had changes.
     *
     * @return the path
     */
    public String getFilePath() {
        return filePath;
    }

    @Override
    public Type<FileContentUpdatedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FileContentUpdatedHandler handler) {
        handler.onFileContentUpdated(this);
    }

    /** Handles  */
    public interface FileContentUpdatedHandler extends EventHandler {
        void onFileContentUpdated(FileContentUpdatedEvent event);
    }
}
