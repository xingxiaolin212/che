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
package org.eclipse.che.api.core.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents JSON RPC result object. Can be constructed out of
 * stringified json object or by passing specific parameters.
 * Use {@link JsonRpcFactory#createResult(Object)},
 * {@link JsonRpcFactory#createResult(String)} or
 * {@link JsonRpcFactory#createResultList(List)}
 * to get an instance of this entity.
 */
public class JsonRpcResult {
    private final static JsonObject EMPTY_OBJECT = new JsonObject();

    private List<JsonElement> resultList;
    private JsonElement       result;

    @AssistedInject
    public JsonRpcResult(@Assisted("message") String message, JsonParser jsonParser) {
        checkNotNull(message, "Message must not be null");
        checkArgument(!message.isEmpty(), "Message must not be empty");

        JsonElement result = jsonParser.parse(message);
        if (result.isJsonArray()) {
            JsonArray jsonArray = result.getAsJsonArray();
            this.resultList = new ArrayList<>(jsonArray.size());
            jsonArray.forEach(it -> this.resultList.add(it));
        } else {
            this.result = jsonParser.parse(message);
        }
    }

    @AssistedInject
    public JsonRpcResult(@Assisted("result") Object result, JsonParser jsonParser) {
        this.result = result == null ? EMPTY_OBJECT : jsonParser.parse(result.toString());
    }

    @AssistedInject
    public JsonRpcResult(@Assisted("result") List<?> result, JsonParser jsonParser) {
        if (result == null || result.isEmpty()) {
            this.resultList = Collections.emptyList();
        } else {
            this.resultList = result.stream().map(Object::toString).map(jsonParser::parse).collect(Collectors.toList());
        }
    }

    public JsonRpcResult() {
        this.result = EMPTY_OBJECT;
    }

    public boolean isEmptyOrAbsent() {
        return (result == null || EMPTY_OBJECT.equals(result)) && (resultList == null || resultList.isEmpty());
    }

    public boolean isArray() {
        return result == null && resultList != null;
    }

    public JsonElement toJsonElement() {
        if (result != null) {
            return result;
        }

        JsonArray jsonArray = new JsonArray();
        resultList.forEach(jsonArray::add);

        return jsonArray;
    }

    public <T> T getAs(Class<T> type) {
        return JsonRpcUtils.getAs(result, type);
    }

    public <T> List<T> getAsListOf(Class<T> type) {
        return JsonRpcUtils.getAsListOf(resultList, type);
    }

    @Override
    public String toString() {
        return toJsonElement().toString();
    }
}
