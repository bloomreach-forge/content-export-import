/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.exim.core.DocumentVariantImportTask;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * {@link DocumentVariantImportTask} implementation using Hippo Repository Workflow APIs.
 */
public class WorkflowDocumentVariantImportTask extends AbstractContentImportTask implements DocumentVariantImportTask {

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public WorkflowDocumentVariantImportTask(final DocumentManager documentManager) {
        super(documentManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNodeBindingItemFilter<ContentItem> getContentNodeBindingItemFilter() {
        if (contentNodeBindingItemFilter == null) {
            DefaultContentNodeJcrBindingItemFilter filter = new DefaultContentNodeJcrBindingItemFilter();
            filter.addPropertyPathExclude(Constants.META_PROP_NODE_LOCALIZED_NAME);
            filter.addPropertyPathExclude(Constants.META_PROP_NODE_PATH);
            filter.addPropertyPathExclude("hippostdpubwf:*");
            filter.addPropertyPathExclude("hippo:availability");
            filter.addPropertyPathExclude("hippo:paths");
            filter.addPropertyPathExclude("hippo:related");
            filter.addPropertyPathExclude("hippostd:holder");
            filter.addPropertyPathExclude("hippostd:state");
            filter.addPropertyPathExclude("hippostd:stateSummary");
            contentNodeBindingItemFilter = filter;
        }

        return contentNodeBindingItemFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createOrUpdateDocumentFromVariantContentNode(ContentNode contentNode, String primaryTypeName,
            String documentLocation, String locale, String localizedName) throws ContentMigrationException {
        String createdOrUpdatedDocumentLocation = null;

        try {
            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(primaryTypeName);
            }

            if (!getDocumentManager().documentExists(documentLocation)) {
                createdOrUpdatedDocumentLocation = createDocumentFromVariantContentNode(primaryTypeName,
                        documentLocation, locale, localizedName);
            }

            createdOrUpdatedDocumentLocation = updateDocumentFromVariantContentNode(documentLocation, contentNode);
        } catch (DocumentManagerException | RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }

        return createdOrUpdatedDocumentLocation;
    }

    @Override
    public Node updateDocumentFromVariantContentNode(final ContentNode contentNode, final Node documentNode) throws ContentMigrationException {

        if (documentNode == null) {
            throw new IllegalArgumentException("Cannot update document: argument 'documentNode' is null (and argument 'contentNode'=" + contentNode + ")");
        }

        try {
            // check types early
            final String primaryTypeName;
            final Node documentHandleNode;
            if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                documentHandleNode = documentNode;
                primaryTypeName = documentHandleNode.getNode(documentHandleNode.getName()).getPrimaryNodeType().getName();
            }
            else if (documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                documentHandleNode = documentNode.getParent();
                primaryTypeName = documentHandleNode.getPrimaryNodeType().getName();
            }
            else {
                throw new IllegalArgumentException("Cannot update document: node " + documentNode.getPath() +
                        " (or its parent) is not a handle but " + documentNode.getPrimaryNodeType().getName());
            }

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(primaryTypeName);
            }

            return updateHandleFromVariantContentNode(documentHandleNode, contentNode);
        } catch (DocumentManagerException | RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }
    }

    /**
     * Create a document at the document handle node path ({@code documentLocation})
     * and returns the created document handle node path.
     * @param primaryTypeName primary node type name of the document to create
     * @param documentLocation document handle node path where the document should be created
     * @param locale locale name for {@code localizedName} which is used as a localized name of the created document
     * @param localizedName localized name of the document to create
     * @return the created document handle node path
     * @throws DocumentManagerException if document creation fails
     * @throws RepositoryException if document creation fails due to unexpected repository error
     */
    protected String createDocumentFromVariantContentNode(String primaryTypeName, String documentLocation,
            String locale, String localizedName) throws DocumentManagerException, RepositoryException {
        documentLocation = StringUtils.removeEnd(documentLocation, "/");
        String[] folderPathAndName = ContentPathUtils.splitToFolderPathAndName(documentLocation);
        String createdDocumentLocation = getDocumentManager().createDocument(folderPathAndName[0], primaryTypeName,
                folderPathAndName[1], locale, localizedName);
        return createdDocumentLocation;
    }

    /**
     * Update the document located under the document handle node path ({@code documentLocation})
     * and returns the document handle node path where the content was updated.
     * @param documentLocation document handle node path
     * @param contentNode source {@link ContentNode} instance containing the document variant content data
     * @return the document handle node path where the content was updated
     * @throws DocumentManagerException if document update fails
     * @throws RepositoryException if document update fails due to unexpected repository error
     */
    protected String updateDocumentFromVariantContentNode(final String documentLocation, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {
        Document editableDocument = null;

        try {
            editableDocument = getDocumentManager().obtainEditableDocument(documentLocation);
            final Node variant = editableDocument.getCheckedOutNode(getDocumentManager().getSession());

            if (getCurrentContentMigrationRecord() != null) {
                final Node handle = HippoNodeUtils.getHippoDocumentHandle(variant);
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

    protected Node updateHandleFromVariantContentNode(final Node documentHandleNode, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {

        Document editableDocument = null;
        Node handle = null;

        try {
            editableDocument = getDocumentManager().obtainEditableDocument(documentHandleNode);
            final Node variant = editableDocument.getCheckedOutNode(getDocumentManager().getSession());
            handle = HippoNodeUtils.getHippoDocumentHandle(variant);

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentId(handle.getIdentifier());
            }

            getContentNodeBinder().bind(variant, contentNode, getContentNodeBindingItemFilter(),
                    getContentValueConverter());
            getDocumentManager().getSession().save();

            getDocumentManager().commitEditableDocument(editableDocument);
        } catch (DocumentManagerException | RepositoryException e) {
            if (editableDocument != null) {
                getDocumentManager().disposeEditableDocument(editableDocument);
            }

            throw e;
        }

        return handle;
    }

}
