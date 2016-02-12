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
 * Content Migration record which is used to keep execution information of a unit of
 * content migration work item.
 */
public class ContentMigrationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Whether or not this unit of work item was processed.
     */
    private boolean processed;

    /**
     * Whether or not this unit of work item was done successfully.
     */
    private boolean succeeded;

    /**
     * Error detail message when it was not done successfully.
     */
    private String errorMessage;

    /**
     * Content primary node type name of the associated subject content processed.
     */
    private String contentType;

    /**
     * Content primary node identifier of the associated subject content processed.
     */
    private String contentId;

    /**
     * Content primary node handle path of the associated subject content processed.
     */
    private String contentPath;

    /**
     * Any extra attributes storing custom data.
     */
    private Map<String, Object> attributes;

    /**
     * Default constructor.
     */
    public ContentMigrationRecord() {
    }

    /**
     * Returns true if the unit of content migration work item in this record was processed.
     * @return true if the unit of content migration work item in this record was processed
     */
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Sets whether or not the unit of content migration work item in this record was processed.
     * @param processed whether or not the unit of content migration work item in this record was processed
     */
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    /**
     * Returns true if the unit of content migration work item in this record was done successfully.
     * @return  true if the unit of content migration work item in this record was done successfully
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * Sets whether or not the unit of content migration work item in this record was done successfully.
     * @param succeeded whether or not the unit of content migration work item in this record was done successfully
     */
    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    /**
     * Returns the error detail message if the unit of content migration work item in this record failed.
     * @return the error detail message if the unit of content migration work item in this record failed
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error detail message if the unit of content migration work item in this record failed.
     * @param errorMessage the error detail message if the unit of content migration work item in this record failed
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the content primary node type name of the associated subject content processed.
     * @return the content primary node type name of the associated subject content processed
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content primary node type name of the associated subject content processing.
     * @param contentType the content primary node type name of the associated subject content processing
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the content node identifier of the associated subject content processed.
     * @return the content primary node identifier of the associated subject content processed
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Sets the content node identifier of the associated subject content processing.
     * @param contentId the content node identifier of the associated subject content processing
     */
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    /**
     * Returns the content path of the associated subject content processed.
     * @return the content path of the associated subject content processed
     */
    public String getContentPath() {
        return contentPath;
    }

    /**
     * Sets the content path of the associated subject content processing.
     * @param contentPath the content path of the associated subject content processing
     */
    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    /**
     * Returns an unmodifiable map of extra custom attributes.
     * @return an unmodifiable map of extra custom attributes
     */
    public Map<String, Object> getAttributeMap() {
        if (attributes == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns an unmodifiable set of extra custom attribute names.
     * @return an unmodifiable set of extra custom attribute names
     */
    public Set<String> getAttributeNames() {
        if (attributes != null) {
            return Collections.unmodifiableSet(attributes.keySet());
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Sets an extra custom attribute.
     * @param name attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, Object value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<String, Object> ();
        }

        attributes.put(name, value);
    }

    /**
     * Finds and returns the attribute value by the {@code name} if found.
     * Otherwise, return null.
     * @param name attribute name
     * @return attribute value if found. Otherwise null.
     */
    public Object getAttribute(String name) {
        if (attributes == null) {
            return null;
        }

        return attributes.get(name);
    }

    /**
     * Gets an attribute value as {@link String} object if found. If not found, returns an empty string instead.
     * This method is useful if you want to retrieve an attribute always as string or empty string value when it is null.
     * @param name attribute name
     * @return an attribute value as {@link String} object if found. If not found, returns an empty string instead
     */
    public String getAttributeAsString(String name) {
        Object obj = getAttribute(name);

        if (obj == null) {
            return StringUtils.EMPTY;
        }

        return obj.toString();
    }

    /**
     * Gets an attribute value as {@link AtomicInteger} object if found.
     * If not found, returns null when {@code create} is false,
     * or sets and returns a new {@link AtomicInteger} object when {@code create} is true.
     * This method is useful if you want to record a counter for some reason while processing data.
     * @param name attribute name
     * @return an attribute value as {@link AtomicInteger} object if found.
     *         If not found, returns null when {@code create} is false,
     *         or sets and returns a new {@link AtomicInteger} object when {@code create} is true.
     */
    public AtomicInteger getAttributeAsAtomicInteger(String name, boolean create) {
        AtomicInteger counter = (AtomicInteger) getAttribute(name);

        if (counter == null && create) {
            counter = new AtomicInteger();
            setAttribute(name, counter);
        }

        return counter;
    }

    /**
     * Gets an attribute value as {@link Collection} object if found.
     * If not found, returns null when {@code create} is false,
     * or sets and returns a new {@link Collection} object when {@code create} is true.
     * This method is useful if you want to add data in the record for some reason while processing data.
     * @param name attribute name
     * @return an attribute value as {@link Collection} object if found.
     *         If not found, returns null when {@code create} is false,
     *         or sets and returns a new {@link Collection} object when {@code create} is true.
     */
    public Collection<Object> getAttributeAsCollection(String name, boolean create) {
        Collection<Object> collection = (Collection<Object>) getAttribute(name);

        if (collection == null && create) {
            collection = new LinkedList<>();
            setAttribute(name, collection);
        }

        return collection;
    }

    /**
     * Removes an attribute by {@code name} and returns the removed attribute value if found.
     * If not found, returns null.
     * @param name attribute name
     * @return removed attribute value if found. If not found, returns null.
     */
    public Object removeAttribute(String name) {
        if (attributes != null) {
            return attributes.remove(name);
        }

        return null;
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
