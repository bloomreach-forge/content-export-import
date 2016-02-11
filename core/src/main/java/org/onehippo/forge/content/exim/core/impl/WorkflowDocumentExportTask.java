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
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentExportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.DefaultJcrContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

public class WorkflowDocumentExportTask extends AbstractContentMigrationTask implements DocumentExportTask {

    private ContentNodeMapper<Node, Item, Value> contentNodeMapper;
    private ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter;

    public WorkflowDocumentExportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    public ContentNodeMapper<Node, Item, Value> getContentNodeMapper() {
        if (contentNodeMapper == null) {
            contentNodeMapper = new DefaultJcrContentNodeMapper();
        }

        return contentNodeMapper;
    }

    public void setContentNodeMapper(ContentNodeMapper<Node, Item, Value> contentNodeMapper) {
        this.contentNodeMapper = contentNodeMapper;
    }

    public ContentNodeMappingItemFilter<Item> getContentNodeMappingItemFilter() {
        if (contentNodeMappingItemFilter == null) {
            contentNodeMappingItemFilter = new DefaultHippoJcrItemMappingFilter();
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter).addPropertyPathExclude("hippostdpubwf:*");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter)
                    .addPropertyPathExclude("hippo:availability");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter).addPropertyPathExclude("hippo:paths");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter).addPropertyPathExclude("hippo:related");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter).addPropertyPathExclude("hippostd:holder");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter).addPropertyPathExclude("hippostd:state");
            ((DefaultHippoJcrItemMappingFilter) contentNodeMappingItemFilter)
                    .addPropertyPathExclude("hippostd:stateSummary");
        }

        return contentNodeMappingItemFilter;
    }

    public void setContentNodeMappingItemFilter(ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter) {
        this.contentNodeMappingItemFilter = contentNodeMappingItemFilter;
    }

    @Override
    public ContentNode exportVariantToContentNode(final Document document) throws ContentMigrationException {
        ContentNode contentNode = null;

        try {
            final Node node = document.getNode(getDocumentManager().getSession());
            getCurrentContentMigrationRecord().setContentType(node.getPrimaryNodeType().getName());
            getCurrentContentMigrationRecord().setContentPath(node.getPath());

            contentNode = getContentNodeMapper().map(node, getContentNodeMappingItemFilter(), getContentValueConverter());
            setMetaProperties(contentNode, node);
        } catch (RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }

        return contentNode;
    }

    protected void setMetaProperties(final ContentNode contentNode, final Node node) throws RepositoryException {
        final Node handle = WorkflowUtils.getHippoDocumentHandle(node);

        if (handle != null) {
            contentNode.setProperty(Constants.META_PROP_NODE_NAME, handle.getName());
            contentNode.setProperty(Constants.META_PROP_NODE_PATH, handle.getPath());

            if (handle instanceof HippoNode) {
                contentNode.setProperty(Constants.META_PROP_NODE_LOCALIZED_NAME,
                        ((HippoNode) handle).getLocalizedName());
            }
        } else {
            contentNode.setProperty(Constants.META_PROP_NODE_NAME, node.getName());
            contentNode.setProperty(Constants.META_PROP_NODE_PATH, node.getPath());

            if (node instanceof HippoNode) {
                contentNode.setProperty(Constants.META_PROP_NODE_LOCALIZED_NAME, ((HippoNode) node).getLocalizedName());
            }
        }
    }
}
