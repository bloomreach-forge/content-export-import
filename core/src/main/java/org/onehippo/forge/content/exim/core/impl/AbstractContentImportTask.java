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
package org.onehippo.forge.content.exim.core.impl;

import javax.jcr.Node;
import javax.jcr.Value;

import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;

/**
 * Abstract content import task implementation class to provide common properties and utility operations.
 */
public class AbstractContentImportTask extends AbstractContentMigrationTask {

    protected ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder;
    protected ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter;

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public AbstractContentImportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    /**
     * Returns {@link ContentNodeBinder} instance. If not set, returns a default implementation.
     * @return {@link ContentNodeBinder} instance. If not set, returns a default implementation
     */
    public ContentNodeBinder<Node, ContentItem, Value> getContentNodeBinder() {
        if (contentNodeBinder == null) {
            contentNodeBinder = new DefaultJcrContentNodeBinder();
        }

        return contentNodeBinder;
    }

    /**
     * Sets {@link ContentNodeBinder} instance.
     * @param contentNodeBinder {@link ContentNodeBinder} instance
     */
    public void setContentNodeBinder(ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder) {
        this.contentNodeBinder = contentNodeBinder;
    }

    /**
     * Returns {@link ContentNodeBindingItemFilter} instance. If not set, returns a default implementation.
     * @return {@link ContentNodeBindingItemFilter} instance. If not set, returns a default implementation
     */
    public ContentNodeBindingItemFilter<ContentItem> getContentNodeBindingItemFilter() {
        if (contentNodeBindingItemFilter == null) {
            contentNodeBindingItemFilter = new DefaultContentNodeJcrBindingItemFilter();
        }

        return contentNodeBindingItemFilter;
    }

    /**
     * Sets {@link ContentNodeBindingItemFilter} instance.
     * @param contentNodeBindingItemFilter {@link ContentNodeBindingItemFilter} instance
     */
    public void setContentNodeBindingItemFilter(
            ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter) {
        this.contentNodeBindingItemFilter = contentNodeBindingItemFilter;
    }

}
