/*
 * Copyright 2016-2024 Bloomreach B.V. (https://www.bloomreach.com)
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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNode;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.DefaultJcrContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Abstract content export task implementation class to provide common properties and utility operations.
 */
abstract public class AbstractContentExportTask extends AbstractContentMigrationTask {

    protected ContentNodeMapper<Node, Item, Value> contentNodeMapper;
    protected ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter;

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public AbstractContentExportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    /**
     * Returns {@link ContentNodeMapper} instance. If not set, returns a default implementation.
     * @return {@link ContentNodeMapper} instance. If not set, returns a default implementation
     */
    public ContentNodeMapper<Node, Item, Value> getContentNodeMapper() {
        if (contentNodeMapper == null) {
            contentNodeMapper = new DefaultJcrContentNodeMapper();
        }

        return contentNodeMapper;
    }

    /**
     * Sets {@link ContentNodeMapper} instance.
     * @param contentNodeMapper {@link ContentNodeMapper} instance
     */
    public void setContentNodeMapper(ContentNodeMapper<Node, Item, Value> contentNodeMapper) {
        this.contentNodeMapper = contentNodeMapper;
    }

    /**
     * Returns {@link ContentNodeMappingItemFilter} instance. If not set, returns a default implementation.
     * @return {@link ContentNodeMappingItemFilter} instance. If not set, returns a default implementation
     */
    public ContentNodeMappingItemFilter<Item> getContentNodeMappingItemFilter() {
        if (contentNodeMappingItemFilter == null) {
            contentNodeMappingItemFilter = new DefaultHippoJcrItemMappingFilter();
        }

        return contentNodeMappingItemFilter;
    }

    /**
     * Sets {@link ContentNodeMappingItemFilter} instance.
     * @param contentNodeMappingItemFilter {@link ContentNodeMappingItemFilter} instance
     */
    public void setContentNodeMappingItemFilter(ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter) {
        this.contentNodeMappingItemFilter = contentNodeMappingItemFilter;
    }

    /**
     * Set meta properties such as {@link Constants#META_PROP_NODE_PATH} and {@link Constants#META_PROP_NODE_LOCALIZED_NAME},
     * which might be helpful when importing back later.
     * @param contentNode {@link ContentNode} instance to set the meta properties
     * @param sourceNode the source node from which the meta properties should be extracted
     * @throws RepositoryException if data cannot be read from the {@code sourceNode} due to repository error
     */
    protected void setMetaProperties(final ContentNode contentNode, final Node sourceNode) throws RepositoryException {
        final Node handle = HippoNodeUtils.getHippoDocumentHandle(sourceNode);

        if (handle != null) {
            contentNode.setProperty(Constants.META_PROP_NODE_PATH,
                    ContentPathUtils.removeIndexNotationInNodePath(handle.getPath()));

            if (handle instanceof HippoNode) {
                contentNode.setProperty(Constants.META_PROP_NODE_LOCALIZED_NAME,
                        ((HippoNode) handle).getDisplayName());
            }
        } else {
            contentNode.setProperty(Constants.META_PROP_NODE_PATH,
                    ContentPathUtils.removeIndexNotationInNodePath(sourceNode.getPath()));

            if (sourceNode instanceof HippoNode) {
                contentNode.setProperty(Constants.META_PROP_NODE_LOCALIZED_NAME, ((HippoNode) sourceNode).getDisplayName());
            }
        }
    }
}
