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

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.forge.content.exim.core.BinaryImportTask;
import org.onehippo.forge.content.exim.core.Constants;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Default {@link BinaryImportTask} implementation.
 */
public class DefaultBinaryImportTask extends AbstractContentImportTask implements BinaryImportTask {

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public DefaultBinaryImportTask(final DocumentManager documentManager) {
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
    public String createOrUpdateBinaryFolder(String folderLocation, String primaryTypeName, String[] folderTypes,
            String[] galleryTypes) throws ContentMigrationException {
        String folderPath = null;

        try {
            final Node folderNode = HippoBinaryNodeUtils.createMissingHippoBinaryFolders(getDocumentManager().getSession(),
                    folderLocation, primaryTypeName, folderTypes, galleryTypes);
            getDocumentManager().getSession().save();
            folderPath = folderNode.getPath();
        } catch (RepositoryException | WorkflowException e) {
            try {
                getDocumentManager().getSession().refresh(false);
            } catch (RepositoryException re) {
                e.printStackTrace();
            }

            throw new ContentMigrationException(e.toString(), e);
        }

        return folderPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createOrUpdateBinaryFromContentNode(ContentNode contentNode, String primaryTypeName,
            String folderPath, String name) throws ContentMigrationException {
        String binaryContentPath = null;
        Node binaryHandleNode = null;

        try {
            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentType(primaryTypeName);
            }

            if (!getDocumentManager().getSession().nodeExists(folderPath)) {
                throw new IllegalArgumentException("Folder doesn't exist at " + folderPath);
            }

            final Node folderNode = getDocumentManager().getSession().getNode(folderPath);

            if (!folderNode.hasNode(name)) {
                binaryHandleNode = createBinaryHandleAndVariantNode(primaryTypeName, folderNode, name);
                binaryContentPath = binaryHandleNode.getPath();
            } else {
                binaryHandleNode = folderNode.getNode(name);
            }

            if (getCurrentContentMigrationRecord() != null) {
                getCurrentContentMigrationRecord().setContentId(binaryHandleNode.getIdentifier());
            }

            updateBinaryHandleAndVariantNodeFromBinaryVariantContentNode(binaryHandleNode, contentNode);

            getDocumentManager().getSession().save();
        } catch (Exception e) {
            try {
                getDocumentManager().getSession().refresh(false);
            } catch (RepositoryException re) {
                e.printStackTrace();
            }

            throw new ContentMigrationException(e.toString(), e);
        }

        return binaryContentPath;
    }

    /**
     * Creates a binary handle and variant node and returns the binary handle node.
     * @param primaryTypeName primary node type name of the binary variant node
     * @param folderNode folder node
     * @param name binary node name
     * @return created binary handle node
     * @throws DocumentManagerException if it fails to create the binary handle and variant node
     * @throws RepositoryException if it fails to create the binary handle and variant node
     */
    protected Node createBinaryHandleAndVariantNode(String primaryTypeName, Node folderNode, String name)
            throws DocumentManagerException, RepositoryException {
        Node handle = folderNode.addNode(name, HippoNodeType.NT_HANDLE);
        handle.addMixin("mix:referenceable");
        Node binarySetNode = handle.addNode(name, primaryTypeName);
        binarySetNode.addMixin("mix:referenceable");
        binarySetNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[] { "live", "preview" });
        return handle;
    }

    /**
     * Update binary handle and variant node from the {@link ContentNode} data containing the binary variant content data.
     * @param binaryHandleNode binary handle node
     * @param contentNode {@link ContentNode} data containing the binary variant content data
     * @throws DocumentManagerException if it fails to update the binary handle and variant node
     * @throws RepositoryException if it fails to update the binary handle and variant node
     */
    protected void updateBinaryHandleAndVariantNodeFromBinaryVariantContentNode(final Node binaryHandleNode,
            final ContentNode contentNode) throws DocumentManagerException, RepositoryException {
        getContentNodeBinder().bind(binaryHandleNode.getNode(binaryHandleNode.getName()), contentNode,
                getContentNodeBindingItemFilter(), getContentValueConverter());
    }

}
