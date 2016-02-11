/*
 * Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.content.exim.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Content Migration record.
 */
public class ContentMigrationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean processed;

    private boolean succeeded;

    private String errorMessage;

    private String contentType;

    private String contentId;

    private String contentPath;

    private Map<String, Object> attributes;

    public ContentMigrationRecord() {
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public Map<String, Object> getAttributeMap() {
        if (attributes == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(attributes);
    }

    public Set<String> getAttributeNames() {
        if (attributes != null) {
            return Collections.unmodifiableSet(attributes.keySet());
        } else {
            return Collections.emptySet();
        }
    }

    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, Object> ();
        }

        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        if (attributes == null) {
            return null;
        }

        return attributes.get(key);
    }

    public String getStringAttribute(String key) {
        Object obj = getAttribute(key);

        if (obj == null) {
            return StringUtils.EMPTY;
        }

        return obj.toString();
    }

    public Object removeAttribute(String key) {
        if (attributes != null) {
            return attributes.remove(key);
        }

        return null;
    }

    public AtomicInteger getCounterAttribute(String key, boolean create) {
        AtomicInteger counter = (AtomicInteger) getAttribute(key);

        if (counter == null && create) {
            counter = new AtomicInteger();
            setAttribute(key, counter);
        }

        return counter;
    }

    public Collection<Object> getCollectionAttribute(String key, boolean create) {
        Collection<Object> collection = (Collection<Object>) getAttribute(key);

        if (collection == null && create) {
            collection = new LinkedList<>();
            setAttribute(key, collection);
        }

        return collection;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this).append("processed", isProcessed())
                .append("succeeded", isSucceeded()).append("contentType", contentType).append("contentId", contentId)
                .append("contentPath", contentPath).append("errorMessage", getErrorMessage());

        final Map<String, Object> attributes = getAttributeMap();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            builder.append(entry.getKey(), entry.getValue());
        }

        return builder.toString();
    }
}
