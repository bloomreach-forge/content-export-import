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
import org.onehippo.forge.content.exim.core.ContentExportException;
import org.onehippo.forge.content.exim.core.ContentExportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.mapper.ContentNodeMapper;
import org.onehippo.forge.content.pojo.mapper.jcr.DefaultJcrContentNodeMapper;
import org.onehippo.forge.content.pojo.model.ContentNode;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HippoWorkflowContentExportTask implements ContentExportTask {

    private final DocumentManager documentManager;
    private final FileObject targetBaseFolder;

    private ContentNodeMapper<Node, Item, Value> contentNodeMapper;
    private ObjectMapper objectMapper;

    public HippoWorkflowContentExportTask(final DocumentManager documentManager, final FileObject targetBaseFolder) {
        this.documentManager = documentManager;
        this.targetBaseFolder = targetBaseFolder;
    }

    public FileObject getTargetBaseFolder() {
        return targetBaseFolder;
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

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    @Override
    public void exportDocumentToJsonFile(Document document, FileObject targetFile) throws ContentExportException {
        OutputStream os = null;
        BufferedOutputStream bos = null;

        try {
            final Node node = document.getNode(getDocumentManager().getSession());
            final ContentNode contentNode = getContentNodeMapper().map(node);
            setMetaProperties(contentNode, node);
            os = targetFile.getContent().getOutputStream();
            bos = new BufferedOutputStream(os);
            getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(bos, contentNode);
        } catch (RepositoryException | IOException e) {
            throw new ContentExportException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
    }

    private void setMetaProperties(final ContentNode contentNode, final Node node) throws RepositoryException {
        final Node handle = HippoWorkflowUtils.getHippoDocumentHandle(node);

        if (handle != null) {
            contentNode.setProperty("jcr:name", handle.getName());
            contentNode.setProperty("jcr:path", handle.getPath());

            if (handle instanceof HippoNode) {
                contentNode.setProperty("jcr:localizedName", ((HippoNode) handle).getLocalizedName());
            }
        } else {
            contentNode.setProperty("jcr:name", node.getName());
            contentNode.setProperty("jcr:path", node.getPath());

            if (node instanceof HippoNode) {
                contentNode.setProperty("jcr:localizedName", ((HippoNode) node).getLocalizedName());
            }
        }
    }
}
