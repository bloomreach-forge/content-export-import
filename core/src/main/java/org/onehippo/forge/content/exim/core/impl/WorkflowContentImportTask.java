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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.exim.core.ContentExportException;
import org.onehippo.forge.content.exim.core.ContentImportException;
import org.onehippo.forge.content.exim.core.ContentImportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

public class WorkflowContentImportTask extends AbstractContentExportImportTask implements ContentImportTask {

    private ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder;
    private ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter;

    public WorkflowContentImportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    public ContentNodeBinder<Node, ContentItem, Value> getContentNodeBinder() {
        if (contentNodeBinder == null) {
            contentNodeBinder = new DefaultJcrContentNodeBinder();
        }

        return contentNodeBinder;
    }

    public void setContentNodeBinder(ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder) {
        this.contentNodeBinder = contentNodeBinder;
    }

    public ContentNodeBindingItemFilter<ContentItem> getContentNodeBindingItemFilter() {
        if (contentNodeBindingItemFilter == null) {
            contentNodeBindingItemFilter = new DefaultContentNodeJcrBindingItemFilter();
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter).addPropertyPathExclude("hippo:*");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostd:*");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostdpubwf:*");
        }

        return contentNodeBindingItemFilter;
    }

    public void setContentNodeBindingItemFilter(
            ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter) {
        this.contentNodeBindingItemFilter = contentNodeBindingItemFilter;
    }

    @Override
    public ContentNode readContentNodeFromJsonFile(final FileObject sourceFile) throws ContentExportException {
        ContentNode contentNode = null;

        InputStream is = null;
        BufferedInputStream bis = null;

        try {
            is = sourceFile.getContent().getInputStream();
            bis = new BufferedInputStream(is);
            contentNode = getObjectMapper().readValue(bis, ContentNode.class);
        } catch (IOException e) {
            throw new ContentImportException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        return contentNode;
    }

    @Override
    public String createOrUpdateDocumentFromVariantContentNode(ContentNode contentNode, String documentLocation,
            String templateCategory, String prototype, String locale, String localizedName)
                    throws ContentExportException {
        String createdOrUpdatedDocumentLocation = null;

        try {
            if (!getDocumentManager().getSession().nodeExists(documentLocation)) {
                createdOrUpdatedDocumentLocation = createDocumentFromVariantContentNode(documentLocation, contentNode,
                        templateCategory, prototype, locale, localizedName);
            }

            createdOrUpdatedDocumentLocation = updateDocumentFromVariantContentNode(documentLocation, contentNode);
        } catch (DocumentManagerException | RepositoryException e) {
            throw new ContentImportException(e.toString(), e);
        }

        return createdOrUpdatedDocumentLocation;
    }

    private String createDocumentFromVariantContentNode(String documentLocation, final ContentNode contentNode,
            String templateCategory, String prototype, String locale, String localizedName)
                    throws DocumentManagerException, RepositoryException {
        documentLocation = StringUtils.removeEnd(documentLocation, "/");
        int offset = StringUtils.lastIndexOf(documentLocation, '/');
        final String folderLocation = StringUtils.substring(documentLocation, 0, offset);
        final String nodeName = StringUtils.substring(documentLocation, offset + 1);
        String createdDocumentLocation = getDocumentManager().createDocument(folderLocation, templateCategory, prototype,
                nodeName, locale, localizedName);
        return createdDocumentLocation;
    }

    private String updateDocumentFromVariantContentNode(final String documentLocation, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {
        Document editableDocument = null;

        try {
            editableDocument = getDocumentManager().obtainEditableDocument(documentLocation);
            final Node variant = editableDocument.getCheckedOutNode(getDocumentManager().getSession());
            getContentNodeBinder().bind(variant, contentNode, getContentNodeBindingItemFilter(),
                    getContentValueConverter());
            getDocumentManager().commitEditableDocument(documentLocation);
        } catch (DocumentManagerException | RepositoryException e) {
            if (editableDocument != null) {
                getDocumentManager().disposeEditableDocument(documentLocation);
            }

            throw e;
        }

        return documentLocation;
    }

}
