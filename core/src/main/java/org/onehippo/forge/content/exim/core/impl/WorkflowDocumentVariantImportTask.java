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
        String createdOrUpdatedDocumentLocation = documentLocation;

        try {
            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(primaryTypeName);
            }

            if (!getDocumentManager().documentExists(documentLocation)) {
                createdOrUpdatedDocumentLocation =
                        createDocument(primaryTypeName, documentLocation, locale, localizedName);
            }

            createdOrUpdatedDocumentLocation = updateDocument(createdOrUpdatedDocumentLocation, contentNode);
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
            final Node documentHandleNode;
            if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                documentHandleNode = documentNode;
            }
            else if (documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                documentHandleNode = documentNode.getParent();
            }
            else {
                throw new IllegalArgumentException("Cannot update document: node " + documentNode.getPath() +
                        " (or its parent) is not a handle but " + documentNode.getPrimaryNodeType().getName());
            }

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(contentNode.getPrimaryType());
            }

            return updateDocument(documentHandleNode, contentNode);
        } catch (DocumentManagerException | RepositoryException e) {
            throw new ContentMigrationException(e.toString(), e);
        }
    }


    /**
     * @deprecated renamed to #createDocument
     */
    @Deprecated
    protected String createDocumentFromVariantContentNode(String primaryTypeName, String documentLocation,
            String locale, String localizedName) throws DocumentManagerException, RepositoryException {
        return createDocument(primaryTypeName, documentLocation, locale, localizedName);
    }
    /**
     * Create a document at the document handle node path ({@code documentLocation})
     * and returns the created document handle node path.
     *
     * @param primaryTypeName primary node type name of the document to create
     * @param documentLocation document handle node path where the document should be created
     * @param locale locale name for {@code localizedName} which is used as a localized name of the created document
     * @param localizedName localized name of the document to create
     * @return the created document handle node path
     * @throws DocumentManagerException if document creation fails
     * @throws RepositoryException if document creation fails due to unexpected repository error
     */
    protected String createDocument(String primaryTypeName, String documentLocation,
            String locale, String localizedName) throws DocumentManagerException, RepositoryException {
        documentLocation = StringUtils.removeEnd(documentLocation, "/");
        String[] folderPathAndName = ContentPathUtils.splitToFolderPathAndName(documentLocation);
        String createdDocumentLocation = getDocumentManager().createDocument(folderPathAndName[0], primaryTypeName,
                folderPathAndName[1], locale, localizedName);
        return createdDocumentLocation;
    }

    /**
     * @deprecated renamed to #updateDocument
     */
    @Deprecated
    protected String updateDocumentFromVariantContentNode(final String documentLocation, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {
        return updateDocument(documentLocation, contentNode);
    }

    /**
     * Update the document located under the document handle node path ({@code documentLocation})
     * and returns the document handle node path where the content was updated.
     *
     * @param documentLocation document handle node path
     * @param contentNode source {@link ContentNode} instance containing the document variant content data
     * @return the document handle node path where the content was updated
     * @throws DocumentManagerException if document update fails
     * @throws RepositoryException if document update fails due to unexpected repository error
     */
    protected String updateDocument(final String documentLocation, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {

        final Document editableDocument = getDocumentManager().obtainEditableDocument(documentLocation);

        updateDocument(editableDocument, contentNode);

        return documentLocation;
    }

    /**
     * Update the document handle node ({@code documentHandleNode}) and returns the document handle node where the
     * content was updated.
     *
     * @param documentHandleNode document handle node
     * @param contentNode source {@link ContentNode} instance containing the document variant content data
     * @return the document handle node where the content was updated
     * @throws DocumentManagerException if document update fails
     * @throws RepositoryException if document update fails due to unexpected repository error
     */
    protected Node updateDocument(final Node documentHandleNode, final ContentNode contentNode)
            throws DocumentManagerException, RepositoryException {

        final Document editableDocument = getDocumentManager().obtainEditableDocument(documentHandleNode);

        return updateDocument(editableDocument, contentNode);
    }

    /**
     * Update the editable document represented by the ({@code editabledocument}) argument and returns the document
     * handle node content was updated.
     *
     * @param editableDocument document object
     * @param contentNode source {@link ContentNode} instance containing the document variant content data
     * @return the document handle node where the content was updated
     * @throws DocumentManagerException if document update fails
     * @throws RepositoryException if document update fails due to unexpected repository error
     */
    protected Node updateDocument(final Document editableDocument, final ContentNode contentNode) throws RepositoryException {

        try {
            final Node variant = editableDocument.getCheckedOutNode(getDocumentManager().getSession());
            final Node handle = HippoNodeUtils.getHippoDocumentHandle(variant);

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentId(handle.getIdentifier());
            }

            getContentNodeBinder().bind(variant, contentNode, getContentNodeBindingItemFilter(),
                    getContentValueConverter());
            getDocumentManager().getSession().save();

            getDocumentManager().commitEditableDocument(editableDocument);

            return handle;
        }
        catch (DocumentManagerException | RepositoryException e) {
            if (editableDocument != null) {
                getDocumentManager().disposeEditableDocument(editableDocument);
            }

            throw e;
        }
    }
}
