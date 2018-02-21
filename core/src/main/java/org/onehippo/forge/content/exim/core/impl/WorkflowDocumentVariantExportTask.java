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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentVariantExportTask;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * {@link DocumentVariantExportTask} implementation using Hippo Repository Workflow APIs.
 */
public class WorkflowDocumentVariantExportTask extends AbstractContentExportTask implements DocumentVariantExportTask {

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public WorkflowDocumentVariantExportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNodeMappingItemFilter<Item> getContentNodeMappingItemFilter() {
        if (contentNodeMappingItemFilter == null) {
            DefaultHippoJcrItemMappingFilter filter = new DefaultHippoJcrItemMappingFilter();
            filter.addPropertyPathExclude("hippostdpubwf:*");
            filter.addPropertyPathExclude("hippo:paths");
            filter.addPropertyPathExclude("hippo:related");
            filter.addPropertyPathExclude("hippostd:holder");
            filter.addPropertyPathExclude("hippostd:state");
            filter.addPropertyPathExclude("hippostd:stateSummary");
            contentNodeMappingItemFilter = filter;
        }

        return contentNodeMappingItemFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNode exportVariantToContentNode(final Document document) throws ContentMigrationException {
        ContentNode contentNode = null;

        try {
            final Node node = document.getNode(getDocumentManager().getSession());

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(node.getPrimaryNodeType().getName());
            }

            contentNode = getContentNodeMapper().map(node, getContentNodeMappingItemFilter(),
                    getContentValueConverter());
            setMetaProperties(contentNode, node);
        } catch (RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }

        return contentNode;
    }

}
