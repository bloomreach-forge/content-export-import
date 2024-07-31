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

import org.onehippo.forge.content.exim.core.BinaryExportTask;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Default {@link BinaryExportTask} implementation.
 */
public class DefaultBinaryExportTask extends AbstractContentExportTask implements BinaryExportTask {

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public DefaultBinaryExportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNodeMappingItemFilter<Item> getContentNodeMappingItemFilter() {
        if (contentNodeMappingItemFilter == null) {
            DefaultHippoJcrItemMappingFilter filter = new DefaultHippoJcrItemMappingFilter();
            filter.addPropertyPathExclude("hippo:availability");
            filter.addPropertyPathExclude("hippo:paths");
            filter.addPropertyPathExclude("hippo:text");
            contentNodeMappingItemFilter = filter;
        }

        return contentNodeMappingItemFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNode exportBinarySetToContentNode(Node imageSetOrAssetSetNode) throws ContentMigrationException {
        ContentNode contentNode = null;

        try {
            final Node node = imageSetOrAssetSetNode;

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(node.getPrimaryNodeType().getName());
            }

            contentNode = getContentNodeMapper().map(node, getContentNodeMappingItemFilter(), getContentValueConverter());
            setMetaProperties(contentNode, node);
        } catch (RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }

        return contentNode;
    }

}
