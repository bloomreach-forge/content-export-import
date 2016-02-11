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
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentVariantImportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

public class WorkflowDocumentVariantImportTask extends AbstractContentMigrationTask implements DocumentVariantImportTask {

    private ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder;
    private ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter;

    public WorkflowDocumentVariantImportTask(final DocumentManager documentManager) {
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
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostdpubwf:*");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippo:availability");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippo:paths");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippo:related");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostd:holder");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostd:state");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostd:stateSummary");
        }

        return contentNodeBindingItemFilter;
    }

    public void setContentNodeBindingItemFilter(
            ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter) {
        this.contentNodeBindingItemFilter = contentNodeBindingItemFilter;
    }

    @Override
    public String createOrUpdateDocumentFromVariantContentNode(ContentNode contentNode, String primaryTypeName,
            String documentLocation, String locale, String localizedName) throws ContentMigrationException {
        String createdOrUpdatedDocumentLocation = null;

        try {
            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(primaryTypeName);
            }

            if (!getDocumentManager().getSession().nodeExists(documentLocation)) {
                createdOrUpdatedDocumentLocation = createDocumentFromVariantContentNode(primaryTypeName,
                        documentLocation, contentNode, locale, localizedName);
            }

            createdOrUpdatedDocumentLocation = updateDocumentFromVariantContentNode(documentLocation, contentNode);
        } catch (DocumentManagerException | RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }

        return createdOrUpdatedDocumentLocation;
    }

    protected String createDocumentFromVariantContentNode(String primaryTypeName, String documentLocation,
            final ContentNode contentNode, String locale, String localizedName)
                    throws DocumentManagerException, RepositoryException {
        documentLocation = StringUtils.removeEnd(documentLocation, "/");
        int offset = StringUtils.lastIndexOf(documentLocation, '/');
        final String folderLocation = StringUtils.substring(documentLocation, 0, offset);
        final String nodeName = StringUtils.substring(documentLocation, offset + 1);
        String createdDocumentLocation = getDocumentManager().createDocument(folderLocation, primaryTypeName, nodeName,
                locale, localizedName);
        return createdDocumentLocation;
    }

    protected String updateDocumentFromVariantContentNode(final String documentLocation, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {
        Document editableDocument = null;

        try {
            editableDocument = getDocumentManager().obtainEditableDocument(documentLocation);
            final Node variant = editableDocument.getCheckedOutNode(getDocumentManager().getSession());

            if (getCurrentContentMigrationRecord() != null) {
                final Node handle = WorkflowUtils.getHippoDocumentHandle(variant);
                getCurrentContentMigrationRecord().setContentId(handle.getIdentifier());
            }

            getContentNodeBinder().bind(variant, contentNode, getContentNodeBindingItemFilter(),
                    getContentValueConverter());
            getDocumentManager().getSession().save();
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
