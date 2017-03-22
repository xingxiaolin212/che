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
package org.eclipse.che.ide.workspace;

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.component.Component;
import org.eclipse.che.ide.api.component.RegistrableComponent;

import java.util.Map;

/**
 * @author Alxander Andrienko
 */
@Singleton
public class WorkspaceRegistrator implements Component {

    @Inject
    public WorkspaceRegistrator(Map<String, Provider<RegistrableComponent>> registrableComponentMap) {

    }

    @Override
    public void start(Callback<Component, Exception> callback) {

    }
}
