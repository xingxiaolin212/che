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
package org.eclipse.che.api.core.jsonrpc.reception;

import org.eclipse.che.api.core.jsonrpc.JsonRpcFactory;
import org.eclipse.che.api.core.jsonrpc.RequestHandler;
import org.eclipse.che.api.core.jsonrpc.RequestHandlerOneToOne;
import org.eclipse.che.api.core.jsonrpc.RequestHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Function configurator to define a function to be applied when we
 * handle incoming JSON RPC request with params object that is
 * represented by a single object while the result of a function is
 * also a single object.
 *
 * @param <P>
 *         type of params object
 * @param <R>
 *         type of result object
 */
public class FunctionConfiguratorOneToOne<P, R> {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionConfiguratorOneToOne.class);

    private final RequestHandlerRegistry registry;
    private final JsonRpcFactory         factory;
    private final String                 method;
    private final Class<P>               pClass;
    private final Class<R>               rClass;

    FunctionConfiguratorOneToOne(RequestHandlerRegistry registry, JsonRpcFactory factory, String method, Class<P> pClass, Class<R> rClass) {
        this.registry = registry;
        this.factory = factory;
        this.method = method;
        this.pClass = pClass;
        this.rClass = rClass;
    }

    /**
     * Define a function to be applied
     *
     * @param function
     *         function
     */
    public void withFunction(BiFunction<String, P, R> function) {
        checkNotNull(function, "Request function must not be null");

        LOG.debug("Configuring incoming request binary function for method: {}, params object class: {}, result object class: {}",
                  method, pClass, rClass);

        RequestHandler handler = new RequestHandlerOneToOne<>(pClass, function, factory);
        registry.register(method, handler);
    }
}
