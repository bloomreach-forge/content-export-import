/*
 * Copyright 2016-2022 Bloomreach B.V. (https://www.bloomreach.com)
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

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.exim.core.DocumentManagerNotFoundException;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for {@link DocumentManager} using Hippo Workflow APIs.
 */
public class WorkflowDocumentManagerImpl implements DocumentManager {

    private Logger logger = LoggerFactory.getLogger(WorkflowDocumentManagerImpl.class);

    private ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder;

    private ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter;

    /**
     * The workflow category name to get a folder workflow. We use threepane as this is the same as the CMS uses
     */
    private String folderWorkflowCategory = "threepane";

    /**
     * The workflow category name to get a document workflow.
     */
    private String documentWorkflowCategory = "default";

    private String defaultWorkflowCategory = "core";

    /**
     * The workflow category name to translate a folder.
     */
    private String folderTranslationWorkflowCategory = "translation";

    /**
     * The workflow category name to translate a document.
     */
    private String documentTranslationWorkflowCategory = "translation";

    private final Session session;

    /**
     * Constructs with {@code session}.
     *
     * @param session JCR session to use
     */
    public WorkflowDocumentManagerImpl(final Session session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns {@link ContentNodeBinder} instance. If not set, returns a default implementation.
     *
     * @return {@link ContentNodeBinder} instance. If not set, returns a default implementation
     */
    public ContentNodeBinder<Node, ContentItem, Value> getContentNodeBinder() {
        if (contentNodeBinder == null) {
            contentNodeBinder = new DefaultJcrContentNodeBinder();
        }

        return contentNodeBinder;
    }

    /**
     * Sets {@link ContentNodeBinder} instance.
     *
     * @param contentNodeBinder {@link ContentNodeBinder} instance
     */
    public void setContentNodeBinder(ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder) {
        this.contentNodeBinder = contentNodeBinder;
    }

    /**
     * Returns {@link ContentNodeBindingItemFilter} instance. If not set, returns a default implementation.
     *
     * @return {@link ContentNodeBindingItemFilter} instance. If not set, returns a default implementation
     */
    public ContentNodeBindingItemFilter<ContentItem> getContentNodeBindingItemFilter() {
        if (contentNodeBindingItemFilter == null) {
            DefaultContentNodeJcrBindingItemFilter bindingItemFilter = new DefaultContentNodeJcrBindingItemFilter();
            bindingItemFilter.addPropertyPathExclude("hippo:*");
            bindingItemFilter.addPropertyPathExclude("hippostd:*");
            bindingItemFilter.addPropertyPathExclude("hippostdpubwf:*");
            contentNodeBindingItemFilter = bindingItemFilter;
        }

        return contentNodeBindingItemFilter;
    }

    /**
     * Sets {@link ContentNodeBindingItemFilter} instance.
     *
     * @param contentNodeBindingItemFilter {@link ContentNodeBindingItemFilter} instance
     */
    public void setContentNodeBindingItemFilter(
            ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter) {
        this.contentNodeBindingItemFilter = contentNodeBindingItemFilter;
    }

    /**
     * Returns the document workflow category.
     *
     * @return the document workflow category
     */
    public String getDocumentWorkflowCategory() {
        return documentWorkflowCategory;
    }

    /**
     * Sets the document workflow category
     *
     * @param documentWorkflowCategory the document workflow category
     */
    public void setDocumentWorkflowCategory(String documentWorkflowCategory) {
        this.documentWorkflowCategory = documentWorkflowCategory;
    }

    /**
     * Returns the default workflow category.
     *
     * @return the default workflow category
     */
    public String getDefaultWorkflowCategory() {
        return defaultWorkflowCategory;
    }

    /**
     * Sets the default workflow category.
     *
     * @param defaultWorkflowCategory the default workflow category
     */
    public void setDefaultWorkflowCategory(String defaultWorkflowCategory) {
        this.defaultWorkflowCategory = defaultWorkflowCategory;
    }

    /**
     * Returns the folder workflow category.
     *
     * @return the folder workflow category
     */
    public String getFolderWorkflowCategory() {
        return folderWorkflowCategory;
    }

    /**
     * Sets the folder workflow category.
     *
     * @param folderWorkflowCategory the folder workflow category
     */
    public void setFolderWorkflowCategory(String folderWorkflowCategory) {
        this.folderWorkflowCategory = folderWorkflowCategory;
    }

    /**
     * Returns the folder translation workflow category.
     *
     * @return the folder translation workflow category
     */
    public String getFolderTranslationWorkflowCategory() {
        return folderTranslationWorkflowCategory;
    }

    /**
     * Sets the folder translation workflow category.
     *
     * @param folderTranslationWorkflowCategory the folder translation workflow category
     */
    public void setFolderTranslationWorkflowCategory(String folderTranslationWorkflowCategory) {
        this.folderTranslationWorkflowCategory = folderTranslationWorkflowCategory;
    }

    /**
     * Returns the document translation workflow category.
     *
     * @return the document translation workflow category
     */
    public String getDocumentTranslationWorkflowCategory() {
        return documentTranslationWorkflowCategory;
    }

    /**
     * Sets the document translation workflow category.
     *
     * @param documentTranslationWorkflowCategory the document translation workflow category
     */
    public void setDocumentTranslationWorkflowCategory(String documentTranslationWorkflowCategory) {
        this.documentTranslationWorkflowCategory = documentTranslationWorkflowCategory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean documentExists(String documentLocation) throws DocumentManagerException {
        try {
            String documentPath = getExistingDocumentPath(documentLocation);

            if (documentPath != null) {
                return true;
            }
        } catch (Exception e) {
            getLogger().error("Failed to check if the document exists at '{}'.", documentLocation, e);
            throw new DocumentManagerException(
                    "Failed to check if the document exists at '" + documentLocation + "'. " + e);
        }

        return false;
    }

    @Override
    public String getExistingDocumentPath(String documentLocation) throws DocumentManagerException {
        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            return (documentHandleNode != null) ? documentHandleNode.getPath() : null;
        } catch (DocumentManagerNotFoundException e) {
            logger.debug("Document is not found at '{}'.", documentLocation);
            return null;
        } catch (DocumentManagerException e) {
            throw e;
        } catch (RepositoryException e) {
            throw new DocumentManagerException("Failed to get the path for '" + documentLocation + "'. " + e, e);
        }
    }

    @Override
    public Node getExistingDocumentHandleNode(String documentLocation) throws DocumentManagerNotFoundException, RepositoryException {
        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location argument: '" + documentLocation + "'.");
        }

        final int offset = documentLocation.lastIndexOf('/');

        if (offset == -1 || offset == documentLocation.length() - 1) {
            throw new IllegalArgumentException("Invalid document location argument: '" + documentLocation + "'.");
        }

        final String folderLocationPart = documentLocation.substring(0, offset);
        final String documentHandleNodeName = HippoNodeUtils.getDefaultUriEncoding()
                .encode(ContentPathUtils.removeIndexNotationInNodePath(documentLocation.substring(offset + 1)));

        final Node folderNode = getExistingFolderNode(folderLocationPart);

        if (folderNode != null) {
            Node handleNode = HippoNodeUtils.getChildNodeOfType(folderNode, documentHandleNodeName,
                    HippoNodeType.NT_HANDLE);

            if (handleNode != null) {
                return handleNode;
            }
        }

        throw new DocumentManagerNotFoundException(
                "Failed to find an existing document at '" + documentLocation + "' (type of hippo:handle).");
    }

    @Override
    public boolean folderExists(String folderLocation) throws DocumentManagerException {
        try {
            String folderPath = getExistingFolderPath(folderLocation);

            if (folderPath != null) {
                return true;
            }
        } catch (Exception e) {
            getLogger().error("Failed to check if the document exists at '{}'.", folderLocation, e);
            throw new DocumentManagerException(
                    "Failed to check if the document exists at '" + folderLocation + "'. " + e);
        }

        return false;
    }

    @Override
    public String getExistingFolderPath(String folderLocation) throws DocumentManagerException {
        try {
            final Node folderNode = getExistingFolderNode(folderLocation);
            return (folderNode != null) ? folderNode.getPath() : null;
        } catch (DocumentManagerNotFoundException e) {
            logger.debug("Folder is not found at '{}'.", folderLocation);
            return null;
        } catch (DocumentManagerException e) {
            throw e;
        } catch (RepositoryException e) {
            throw new DocumentManagerException("Failed to get the path for '" + folderLocation + "'. " + e, e);
        }
    }

    @Override
    public Node getExistingFolderNode(String folderLocation) throws DocumentManagerNotFoundException, RepositoryException {
        if (StringUtils.isBlank(folderLocation)) {
            throw new IllegalArgumentException("Invalid folder location argument: '" + folderLocation + "'.");
        }

        String[] pathSegments = StringUtils.split(
                ContentPathUtils.encodeNodePath(ContentPathUtils.removeIndexNotationInNodePath(folderLocation)), "/");
        Node curFolder = getSession().getRootNode();

        for (int i = 0; i < pathSegments.length - 1; i++) {
            curFolder = HippoNodeUtils.getChildNodeOfType(curFolder, pathSegments[i], HippoStdNodeType.NT_FOLDER,
                    HippoStdNodeType.NT_DIRECTORY);

            if (curFolder == null) {
                throw new DocumentManagerNotFoundException("Failed to find an existing folder at '" + folderLocation
                        + "}' as the interim folder " + "(type of either hippostd:folder or hippostd:directory) at '/"
                        + StringUtils.join(pathSegments, "/", 0, i) + "' doesn't exist.");
            }
        }

        curFolder = HippoNodeUtils.getChildNodeOfType(curFolder, pathSegments[pathSegments.length - 1],
                HippoStdNodeType.NT_FOLDER);

        if (curFolder == null) {
            throw new DocumentManagerNotFoundException(
                    "Failed to find an existing folder at '" + folderLocation + "' (type of hippostd:folder).");
        }

        return curFolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createDocument(String folderLocation, String primaryTypeName, String nodeName, String locale,
            String localizedName) throws DocumentManagerException {
        getLogger().debug("##### createDocument under '{}')", folderLocation);

        String createdDocPath = null;

        try {
            String existingFolderPath = getExistingFolderPath(folderLocation);
            Node folderNode = null;

            if (existingFolderPath != null) {
                folderNode = getSession().getNode(existingFolderPath);
            } else {
                folderNode = HippoNodeUtils.createMissingHippoFolders(getSession(),
                        ContentPathUtils.encodeNodePath(ContentPathUtils.removeIndexNotationInNodePath(folderLocation)));

                if (folderNode == null) {
                    throw new IllegalArgumentException("Folder is not available at '" + folderLocation + "'.");
                }

                if (!folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                    throw new IllegalStateException("Invalid folder found at '" + folderLocation + "', not 'hippostd:folder' type.");
                }
            }

            final FolderWorkflow folderWorkflow = getFolderWorkflow(folderNode);

            Boolean add = (Boolean) folderWorkflow.hints().get("add");

            if (BooleanUtils.isTrue(add)) {
                final String addedDocPath = folderWorkflow.add("new-document", primaryTypeName,
                        ContentPathUtils.encodeNodePath(ContentPathUtils.removeIndexNotationInNodePath(nodeName)));
                final Node handle = HippoNodeUtils.getHippoDocumentHandle(getSession().getNode(addedDocPath));
                final DefaultWorkflow defaultWorkflow = getDefaultWorkflow(handle);
                defaultWorkflow.setDisplayName(localizedName);
                createdDocPath = handle.getPath();
            } else {
                throw new IllegalStateException("Folder at '" + folderLocation + "' is not allowed to add a document.");
            }
        } catch (Exception e) {
            getLogger().error("Failed to add a document with '{}' under '{}'.", nodeName, folderLocation, e);
            throw new DocumentManagerException(
                    "Failed to add a document with '" + nodeName + "' under '" + folderLocation + "'." + "'. " + e, e);
        }

        return createdDocPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document obtainEditableDocument(final String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### obtainEditableDocument for location {}", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            return obtainEditableDocument(documentHandleNode);
        } catch (RepositoryException e) {
            getLogger().error("Failed to obtain editable instance on document.", e);
            throw new DocumentManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document obtainEditableDocument(final Node documentHandleNode) throws DocumentManagerException {

        if (documentHandleNode == null) {
            throw new IllegalArgumentException("Document handle node may not be null");
        }

        String handlePath = "";
        try {
            handlePath = documentHandleNode.getPath();
            getLogger().debug("##### obtainEditableDocument for path {}", handlePath);

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            final Boolean obtainEditableInstance = (Boolean)documentWorkflow.hints().get("obtainEditableInstance");

            if (BooleanUtils.isTrue(obtainEditableInstance)) {
                return documentWorkflow.obtainEditableInstance();
            } else {
                throw new IllegalStateException("Document at '" + handlePath + "' is not allowed to obtain an editable instance.");
            }
        } catch (Exception e) {
            final String message = "Failed to obtain editable instance for document '" + handlePath + "'";
            getLogger().error(message, e);
            throw new DocumentManagerException(message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEditableDocument(Document editableDocument, ContentNode sourceContentNode)
            throws DocumentManagerException {
        try {
            final Node editableDocumentNode = editableDocument.getNode(getSession());
            getContentNodeBinder().bind(editableDocumentNode, sourceContentNode, getContentNodeBindingItemFilter());
        } catch (Exception e) {
            getLogger().error("Failed to update editable document.", e);
            throw new DocumentManagerException(
                    "Failed to obtain editable instance on document (id: '" + editableDocument + "'). " + e, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document disposeEditableDocument(final String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### disposeEditableDocument for location {}", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            return disposeEditableDocument(documentHandleNode);
        } catch (Exception e) {
            getLogger().error("Failed to dispose editable instance on document.", e);
            throw new DocumentManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document disposeEditableDocument(final Document editableDocument) throws DocumentManagerException {

        if (editableDocument == null) {
            throw new IllegalArgumentException("Document object may not be null");
        }

        try {
            final Node variant = editableDocument.getNode(getSession());
            final Node handle = HippoNodeUtils.getHippoDocumentHandle(variant);
            getLogger().debug("##### disposeEditableDocument for path {}", handle.getPath());

            return disposeEditableDocument(handle);
        } catch (RepositoryException e) {
            getLogger().error("Failed to dispose editable instance on document.", e);
            throw new DocumentManagerException(e);
        }
    }

    /**
     * Discards the draft variant which is currently being edited.
     */
    protected Document disposeEditableDocument(final Node documentHandleNode) throws DocumentManagerException {

        if (documentHandleNode == null) {
            throw new IllegalArgumentException("Document handle node may not be null");
        }

        String handlePath = "";
        try {
            handlePath = documentHandleNode.getPath();

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            final Boolean disposeEditableInstance = (Boolean)documentWorkflow.hints().get("disposeEditableInstance");

            if (BooleanUtils.isTrue(disposeEditableInstance)) {
                return documentWorkflow.disposeEditableInstance();
            } else {
                throw new IllegalStateException(
                        "Document at '" + handlePath + "' is not allowed to dispose an editable instance.");
            }
        } catch (Exception e) {
            final String message = "Failed to dispose editable instance on document '" + handlePath + "'";
            getLogger().error(message, e);
            throw new DocumentManagerException(message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document commitEditableDocument(String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### commitEditableDocument for location {}", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        try {
            final Node hanlde = getExistingDocumentHandleNode(documentLocation);
            return commitEditableDocument(hanlde);
        } catch (RepositoryException e) {
            getLogger().error("Failed to commit editable instance", e);
            throw new DocumentManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document commitEditableDocument(Document editableDocument) throws DocumentManagerException {

        if (editableDocument == null) {
            throw new IllegalArgumentException("Document object may not be null");
        }

        try {
            final Node variant = editableDocument.getNode(getSession());
            final Node handle = HippoNodeUtils.getHippoDocumentHandle(variant);

            getLogger().debug("##### commitEditableDocument for {}", (handle == null) ? "null" : handle.getPath());
            return commitEditableDocument(handle);
        } catch (RepositoryException e) {
            getLogger().error("Failed to commit editable instance", e);
            throw new DocumentManagerException(e);
        }
    }

    /**
     * Commits the draft variant which is currently being edited.
     */
    protected Document commitEditableDocument(final Node documentHandleNode) throws DocumentManagerException {

        if (documentHandleNode == null) {
            throw new IllegalArgumentException("Document handle node may not be null");
        }

        String handlePath = "";
        try {
            handlePath = documentHandleNode.getPath();

            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            final Boolean commitEditableInstance = (Boolean)documentWorkflow.hints().get("commitEditableInstance");

            if (BooleanUtils.isTrue(commitEditableInstance)) {
                return documentWorkflow.commitEditableInstance();
            } else {
                throw new IllegalStateException(
                        "Document at '" + handlePath + "' is not allowed to commit an editable instance.");
            }
        } catch (Exception e) {
            final String message = "Failed to commit editable instance on document '" + handlePath + "'";
            getLogger().error(message, e);
            throw new DocumentManagerException(message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean publishDocument(String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### publishDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        boolean published = false;

        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            Boolean publish = (Boolean) documentWorkflow.hints().get("publish");

            if (!BooleanUtils.isTrue(publish)) {
                throw new IllegalStateException("Document at '" + documentLocation + "' doesn't have publish action.");
            }

            documentWorkflow.publish();
            published = true;
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to publish document at '{}'.", documentLocation, e);
            throw new DocumentManagerException("Failed to publish document at '" + documentLocation + "'. " + e, e);
        }

        return published;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean depublishDocument(String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### depublishDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        boolean depublished = false;

        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            Boolean isLive = (Boolean) documentWorkflow.hints().get("isLive");

            if (BooleanUtils.isFalse(isLive)) {
                // already offline, so just return true
                depublished = true;
            } else {
                Boolean depublish = (Boolean) documentWorkflow.hints().get("depublish");

                if (!BooleanUtils.isTrue(depublish)) {
                    throw new IllegalStateException(
                            "Document at '" + documentLocation + "' doesn't have depublish action.");
                }

                documentWorkflow.depublish();
                depublished = true;
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to depublish document at '{}'.", documentLocation, e);
            throw new DocumentManagerException("Failed to depublish document at '" + documentLocation + "'. " + e, e);
        }

        return depublished;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDocument(String documentLocation) throws DocumentManagerException {
        getLogger().debug("##### deleteDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        try {
            final Node documentHandleNode = getExistingDocumentHandleNode(documentLocation);
            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);
            Boolean delete = (Boolean) documentWorkflow.hints().get("delete");

            if (BooleanUtils.isTrue(delete)) {
                documentWorkflow.delete();
            } else {
                throw new IllegalStateException("Document at '" + documentLocation + "' is not allowed to delete.");
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to depublish document at '{}'.", documentLocation, e);
            throw new DocumentManagerException("Failed to depublish document at '" + documentLocation + "'. " + e, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String copyDocument(String sourceDocumentLocation, String targetFolderLocation,
            String targetDocumentNodeName) throws DocumentManagerException {
        getLogger().debug("##### copyDocument('{}', '{}', '{}')", sourceDocumentLocation, targetFolderLocation,
                targetDocumentNodeName);

        String targetDocumentLocation = null;

        try {
            final Node sourceDocumentHandleNode = getExistingDocumentHandleNode(sourceDocumentLocation);
            final Node targetFolderNode = HippoNodeUtils.createMissingHippoFolders(getSession(), ContentPathUtils
                    .encodeNodePath(ContentPathUtils.removeIndexNotationInNodePath(targetFolderLocation)));

            if (targetFolderNode == null) {
                throw new IllegalArgumentException("Target folder doesn't exist at '" + targetFolderLocation + "'.");
            }

            if (!targetFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                throw new IllegalStateException("Invalid folder found at '" + targetFolderLocation + "', not 'hippostd:folder' type.");
            }

            DocumentWorkflow documentWorkflow = getDocumentWorkflow(sourceDocumentHandleNode);
            Boolean copy = (Boolean) documentWorkflow.hints().get("copy");

            if (BooleanUtils.isTrue(copy)) {
                documentWorkflow.copy(new Document(targetFolderNode), targetDocumentNodeName);
                targetDocumentLocation = targetFolderNode.getNode(targetDocumentNodeName).getPath();
            } else {
                throw new IllegalStateException("Copy action not available on document at '" + sourceDocumentLocation
                        + "' to '" + targetFolderLocation + "/" + targetDocumentNodeName + "'.");
            }
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to copy document at '{}' to '{}/{}'.", sourceDocumentLocation,
                    targetFolderLocation, targetDocumentNodeName, e);
            throw new DocumentManagerException("Failed to copy document at '" + sourceDocumentLocation + "' to '"
                    + targetFolderLocation + "/" + targetDocumentNodeName + "'. " + e, e);
        }

        return targetDocumentLocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document translateFolder(String sourceFolderLocation, String targetLanguage, String targetFolderNodeName)
            throws DocumentManagerException {
        getLogger().debug("##### translateFolder('{}', '{}', '{}')", sourceFolderLocation, targetLanguage,
                targetFolderNodeName);

        Document translatedFolderDocument = null;

        try {
            final Node sourceFolderNode = getExistingFolderNode(sourceFolderLocation);
            TranslationWorkflow folderTranslationWorkflow = getFolderTranslationWorkflow(sourceFolderNode);
            translatedFolderDocument = folderTranslationWorkflow.addTranslation(targetLanguage, targetFolderNodeName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to translate folder at '{}' to '{}' in '{}'.", sourceFolderLocation,
                    targetFolderNodeName, targetLanguage, e);
            throw new DocumentManagerException("Failed to add translated folder of '" + sourceFolderLocation + "' to '"
                    + targetFolderNodeName + "' in '" + targetLanguage + "'. " + e, e);
        }

        return translatedFolderDocument;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document translateDocument(String sourceDocumentLocation, String targetLanguage,
            String targetDocumentNodeName) throws DocumentManagerException {
        getLogger().debug("##### translateDocument('{}', '{}', '{}')", sourceDocumentLocation, targetLanguage,
                targetDocumentNodeName);

        Document translatedDocument = null;

        try {
            final Node sourceDocumentHandleNode = getExistingDocumentHandleNode(sourceDocumentLocation);

            Node translationVariantNode = null;
            Map<String, Node> documentVariantsMap = HippoNodeUtils.getDocumentVariantsMap(sourceDocumentHandleNode);

            if (documentVariantsMap.containsKey(HippoStdNodeType.UNPUBLISHED)) {
                translationVariantNode = documentVariantsMap.get(HippoStdNodeType.UNPUBLISHED);
            } else if (documentVariantsMap.containsKey(HippoStdNodeType.PUBLISHED)) {
                translationVariantNode = documentVariantsMap.get(HippoStdNodeType.PUBLISHED);
            }

            if (translationVariantNode == null) {
                throw new IllegalStateException("No available unpublished or published variant in document at '"
                        + sourceDocumentLocation + "'.");
            }

            TranslationWorkflow documentTranslationWorkflow = getDocumentVariantTranslationWorkflow(
                    translationVariantNode);
            translatedDocument = documentTranslationWorkflow.addTranslation(targetLanguage, targetDocumentNodeName);
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            getLogger().error("Failed to translate document at '{}' to '{}' in '{}'.", sourceDocumentLocation,
                    targetDocumentNodeName, targetLanguage, e);
            throw new DocumentManagerException("Failed to add translated document of '" + sourceDocumentLocation
                    + "' to '" + targetDocumentNodeName + "' in '" + targetLanguage + "'. " + e);
        }

        return translatedDocument;
    }

    /**
     * {@inheritDoc}
     *
     * @param documentHandleNode document handle node
     * @return a document workflow on {@code documentHandleNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    @Override
    public DocumentWorkflow getDocumentWorkflow(final Node documentHandleNode) throws RepositoryException {
        return (DocumentWorkflow) HippoNodeUtils.getHippoWorkflow(getSession(), getDocumentWorkflowCategory(),
                documentHandleNode);
    }

    /**
     * Returns a folder workflow instance on {@code folderNode}.
     *
     * @param folderNode folder node
     * @return a folder workflow instance on {@code folderNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    protected FolderWorkflow getFolderWorkflow(final Node folderNode) throws RepositoryException {
        return (FolderWorkflow) HippoNodeUtils.getHippoWorkflow(getSession(), getFolderWorkflowCategory(), folderNode);
    }

    /**
     * Returns a {@link DefaultWorkflow} instance on {@code documentHandleNode}.
     *
     * @param documentHandleNode document handle node
     * @return {@link DefaultWorkflow} instance on {@code documentHandleNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    protected DefaultWorkflow getDefaultWorkflow(final Node documentHandleNode) throws RepositoryException {
        return (DefaultWorkflow) HippoNodeUtils.getHippoWorkflow(getSession(), getDefaultWorkflowCategory(),
                documentHandleNode);
    }

    /**
     * Returns a folder {@link TranslationWorkflow} instance on {@code folderNode}.
     *
     * @param folderNode folder node
     * @return a folder {@link TranslationWorkflow} instance on {@code folderNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    protected TranslationWorkflow getFolderTranslationWorkflow(final Node folderNode) throws RepositoryException {
        return (TranslationWorkflow) HippoNodeUtils.getHippoWorkflow(getSession(),
                getFolderTranslationWorkflowCategory(), folderNode);
    }

    /**
     * Returns a document {@link TranslationWorkflow} instance on {@code documentVariantNode}.
     *
     * @param documentVariantNode document variant node
     * @return a document {@link TranslationWorkflow} instance on {@code documentVariantNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    protected TranslationWorkflow getDocumentVariantTranslationWorkflow(final Node documentVariantNode)
            throws RepositoryException {
        return (TranslationWorkflow) HippoNodeUtils.getHippoWorkflow(getSession(),
                getDocumentTranslationWorkflowCategory(), documentVariantNode);
    }

}
