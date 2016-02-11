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

import org.hippoecm.repository.api.HippoNode;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.DefaultJcrContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

abstract public class AbstractContentExportTask extends AbstractContentMigrationTask {

    protected ContentNodeMapper<Node, Item, Value> contentNodeMapper;
    protected ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter;

    public AbstractContentExportTask(final DocumentManager documentManager) {
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
        }

        return contentNodeMappingItemFilter;
    }

    public void setContentNodeMappingItemFilter(ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter) {
        this.contentNodeMappingItemFilter = contentNodeMappingItemFilter;
    }

    protected void setMetaProperties(final ContentNode contentNode, final Node node) throws RepositoryException {
        final Node handle = HippoWorkflowUtils.getHippoDocumentHandle(node);

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
