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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.ContentExportException;
import org.onehippo.forge.content.exim.core.ContentExportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMappingItemFilter;
import org.onehippo.forge.content.pojo.mapper.jcr.DefaultJcrContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.jcr.hippo.DefaultHippoJcrItemMappingFilter;
import org.onehippo.forge.content.pojo.model.ContentNode;

public class WorkflowContentExportTask extends AbstractContentMigrationTask implements ContentExportTask {

    private ContentNodeMapper<Node, Item, Value> contentNodeMapper;
    private ContentNodeMappingItemFilter<Item> contentNodeMappingItemFilter;

    public WorkflowContentExportTask(final DocumentManager documentManager) {
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

    @Override
    public ContentNode exportVariantToContentNode(final Document document) throws ContentExportException {
        ContentNode contentNode = null;

        try {
            final Node node = document.getNode(getDocumentManager().getSession());
            getCurrentContentMigrationRecord().setContentType(node.getPrimaryNodeType().getName());
            getCurrentContentMigrationRecord().setContentPath(node.getPath());

            contentNode = getContentNodeMapper().map(node, getContentNodeMappingItemFilter(), getContentValueConverter());
            setMetaProperties(contentNode, node);
        } catch (RepositoryException e) {
            throw new ContentExportException(e.toString(), e);
        }

        return contentNode;
    }

    @Override
    public void writeContentNodeToJsonFile(final ContentNode contentNode, final FileObject targetFile) throws ContentExportException {
        OutputStream os = null;
        BufferedOutputStream bos = null;

        try {
            os = targetFile.getContent().getOutputStream();
            bos = new BufferedOutputStream(os);
            getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(bos, contentNode);
        } catch (IOException e) {
            throw new ContentExportException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
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
