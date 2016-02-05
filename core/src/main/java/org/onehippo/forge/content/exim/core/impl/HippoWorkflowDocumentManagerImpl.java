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

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation for {@link DocumentManager} using Hippo Workflow APIs.
 */
public class HippoWorkflowDocumentManagerImpl implements DocumentManager {

    private static Logger log = LoggerFactory.getLogger(HippoWorkflowDocumentManagerImpl.class);

    /**
     * The workflow category name to get a document workflow. 
     */
    private String documentWorkflowCategory = "default";

    private final Session session;

    public HippoWorkflowDocumentManagerImpl(final Session session) {
        this.session = session;
    }

    public String getDocumentWorkflowCategory() {
        return documentWorkflowCategory;
    }

    public void setDocumentWorkflowCategory(String documentWorkflowCategory) {
        this.documentWorkflowCategory = documentWorkflowCategory;
    }

    @Override
    public String obtainEditableDocument(String documentLocation) throws DocumentManagerException {
        log.debug("##### obtainEditableDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        String identifier = null;

        try {
            if (!getSession().nodeExists(documentLocation)) {
                throw new IllegalArgumentException("Document doesn't exist at '" + documentLocation + "'.");
            }

            Node documentHandleNode = HippoWorkflowUtils.getHippoDocumentHandle(getSession().getNode(documentLocation));

            if (documentHandleNode == null) {
                throw new IllegalArgumentException("Document handle is not found at '" + documentLocation + "'.");
            }

            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);

            Boolean obtainEditableInstance = (Boolean) documentWorkflow.hints().get("obtainEditableInstance");

            if (BooleanUtils.isTrue(obtainEditableInstance)) {
                Document document = documentWorkflow.obtainEditableInstance();
                identifier = document.getIdentity();
            } else {
                throw new IllegalStateException(
                        "Document at '" + documentLocation + "' is not allowed to obtain an editable instance.");
            }
        } catch (Exception e) {
            log.error("Failed to obtain editable instance on document.", e);
            throw new DocumentManagerException(
                    "Failed to obtain editable instance on document at '" + documentLocation + "'. " + e);
        }

        return identifier;
    }

    @Override
    public void updateEditableDocument(String documentIdentifier, ContentNode sourceContentNode) throws DocumentManagerException {
        // TODO
    }

    @Override
    public String disposeEditableDocument(String documentLocation) throws DocumentManagerException {
        log.debug("##### disposeEditableDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        String identifier = null;

        try {
            if (!getSession().nodeExists(documentLocation)) {
                throw new IllegalArgumentException("Document doesn't exist at '" + documentLocation + "'.");
            }

            Node documentHandleNode = HippoWorkflowUtils.getHippoDocumentHandle(getSession().getNode(documentLocation));

            if (documentHandleNode == null) {
                throw new IllegalArgumentException("Document handle is not found at '" + documentLocation + "'.");
            }

            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);

            Boolean disposeEditableInstance = (Boolean) documentWorkflow.hints().get("disposeEditableInstance");

            if (BooleanUtils.isTrue(disposeEditableInstance)) {
                Document document = documentWorkflow.disposeEditableInstance();
                identifier = document.getIdentity();
            } else {
                throw new IllegalStateException(
                        "Document at '" + documentLocation + "' is not allowed to dispose an editable instance.");
            }
        } catch (Exception e) {
            log.error("Failed to dispose editable instance on document.", e);
            throw new DocumentManagerException(
                    "Failed to dispose editable instance on document at '" + documentLocation + "'. " + e);
        }

        return identifier;
    }

    @Override
    public String commitEditableDocument(String documentLocation) throws DocumentManagerException {
        log.debug("##### commitEditableDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        String identifier = null;

        try {
            if (!getSession().nodeExists(documentLocation)) {
                throw new IllegalArgumentException("Document doesn't exist at '" + documentLocation + "'.");
            }

            Node documentHandleNode = HippoWorkflowUtils.getHippoDocumentHandle(getSession().getNode(documentLocation));

            if (documentHandleNode == null) {
                throw new IllegalArgumentException("Document handle is not found at '" + documentLocation + "'.");
            }

            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);

            Boolean commitEditableInstance = (Boolean) documentWorkflow.hints().get("commitEditableInstance");

            if (BooleanUtils.isTrue(commitEditableInstance)) {
                Document document = documentWorkflow.commitEditableInstance();
                identifier = document.getIdentity();
            } else {
                throw new IllegalStateException(
                        "Document at '" + documentLocation + "' is not allowed to commit an editable instance.");
            }
        } catch (Exception e) {
            log.error("Failed to commit editable instance on document.", e);
            throw new DocumentManagerException(
                    "Failed to commit editable instance on document at '" + documentLocation + "'. " + e);
        }

        return identifier;
    }

    @Override
    public boolean depublishDocument(String documentLocation) throws DocumentManagerException {
        log.debug("##### depublishDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        boolean depublished = false;

        try {
            if (!getSession().nodeExists(documentLocation)) {
                throw new IllegalArgumentException("Document doesn't exist at '" + documentLocation + "'.");
            }

            Node documentHandleNode = HippoWorkflowUtils.getHippoDocumentHandle(getSession().getNode(documentLocation));

            if (documentHandleNode == null) {
                throw new IllegalArgumentException("Document handle is not found at '" + documentLocation + "'.");
            }

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
            log.error("Failed to depublish document at '{}'.", documentLocation, e);
            throw new DocumentManagerException("Failed to depublish document at '" + documentLocation + "'. " + e);
        }

        return depublished;
    }

    @Override
    public boolean publishDocument(String documentLocation) throws DocumentManagerException {
        log.debug("##### publishDocument('{}')", documentLocation);

        if (StringUtils.isBlank(documentLocation)) {
            throw new IllegalArgumentException("Invalid document location: '" + documentLocation + "'.");
        }

        boolean published = false;

        try {
            if (!getSession().nodeExists(documentLocation)) {
                throw new IllegalArgumentException("Document doesn't exist at '" + documentLocation + "'.");
            }

            Node documentHandleNode = HippoWorkflowUtils.getHippoDocumentHandle(getSession().getNode(documentLocation));

            if (documentHandleNode == null) {
                throw new IllegalArgumentException("Document handle is not found at '" + documentLocation + "'.");
            }

            DocumentWorkflow documentWorkflow = getDocumentWorkflow(documentHandleNode);

            Boolean publish = (Boolean) documentWorkflow.hints().get("publish");

            if (!BooleanUtils.isTrue(publish)) {
                throw new IllegalStateException("Document at '" + documentLocation + "' doesn't have publish action.");
            }

            documentWorkflow.publish();
            published = true;
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.error("Failed to publish document at '{}'.", documentLocation, e);
            throw new DocumentManagerException("Failed to publish document at '" + documentLocation + "'. " + e);
        }

        return published;
    }

    @Override
    public String copyDocument(String sourceDocumentLocation, String targetFolderLocation,
            String targetDocumentNodeName) throws DocumentManagerException {
        log.debug("##### copyDocument('{}', '{}', '{}')", sourceDocumentLocation, targetFolderLocation,
                targetDocumentNodeName);

        String targetDocumentLocation = null;

        try {
            if (!getSession().nodeExists(sourceDocumentLocation)) {
                throw new IllegalArgumentException(
                        "Source document doesn't exist at '" + sourceDocumentLocation + "'.");
            }

            final Node targetFolderNode = HippoWorkflowUtils.createMissingHippoFolders(getSession(),
                    targetFolderLocation);

            if (targetFolderNode == null) {
                throw new IllegalArgumentException("Target folder doesn't exist at '" + targetFolderLocation + "'.");
            }

            Node sourceDocumentHandleNode = HippoWorkflowUtils
                    .getHippoDocumentHandle(getSession().getNode(sourceDocumentLocation));

            if (sourceDocumentHandleNode == null) {
                throw new IllegalArgumentException(
                        "Source document handle is not found at '" + sourceDocumentLocation + "'.");
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
            log.error("Failed to copy document at '{}' to '{}/{}'.", sourceDocumentLocation, targetFolderLocation,
                    targetDocumentNodeName, e);
            throw new DocumentManagerException("Failed to copy document at '" + sourceDocumentLocation + "' to '"
                    + targetFolderLocation + "/" + targetDocumentNodeName + "'. " + e);
        }

        return targetDocumentLocation;
    }

    @Override
    public String translateFolder(String sourceFolderLocation, String targetLanguage, String targetFolderNodeName)
            throws DocumentManagerException {
        log.debug("##### translateFolder('{}', '{}', '{}')", sourceFolderLocation, targetLanguage,
                targetFolderNodeName);

        String targetFolderLocation = null;

        try {
            if (!getSession().nodeExists(sourceFolderLocation)) {
                throw new IllegalArgumentException("Source folder doesn't exist at '" + sourceFolderLocation + "'.");
            }

            Node sourceFolderNode = getSession().getNode(sourceFolderLocation);

            if (sourceFolderNode == null || !sourceFolderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                throw new IllegalArgumentException("Source folder is not found at '" + sourceFolderLocation + "'.");
            }

            TranslationWorkflow folderTranslationWorkflow = getFolderTranslationWorkflow(sourceFolderNode);
            Document translatedFolderDocument = folderTranslationWorkflow.addTranslation(targetLanguage,
                    targetFolderNodeName);
            Node translatedFolderNode = translatedFolderDocument.getNode(getSession());
            targetFolderLocation = translatedFolderNode.getPath();
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.error("Failed to translate folder at '{}' to '{}' in '{}'.", sourceFolderLocation, targetFolderNodeName,
                    targetLanguage, e);
            throw new DocumentManagerException("Failed to add translated folder of '" + sourceFolderLocation + "' to '"
                    + targetFolderNodeName + "' in '" + targetLanguage + "'. " + e);
        }

        return targetFolderLocation;
    }

    @Override
    public String translateDocument(String sourceDocumentLocation, String targetLanguage, String targetDocumentNodeName)
            throws DocumentManagerException {
        log.debug("##### translateDocument('{}', '{}', '{}')", sourceDocumentLocation, targetLanguage,
                targetDocumentNodeName);

        String targetDocumentLocation = null;

        try {
            if (!getSession().nodeExists(sourceDocumentLocation)) {
                throw new IllegalArgumentException(
                        "Source document doesn't exist at '" + sourceDocumentLocation + "'.");
            }

            Node sourceDocumentHandleNode = HippoWorkflowUtils
                    .getHippoDocumentHandle(getSession().getNode(sourceDocumentLocation));

            if (sourceDocumentHandleNode == null) {
                throw new IllegalArgumentException(
                        "Source document handle is not found at '" + sourceDocumentLocation + "'.");
            }

            Node translationVariantNode = null;
            Map<String, Node> documentVariantsMap = HippoWorkflowUtils.getDocumentVariantsMap(sourceDocumentHandleNode);

            if (documentVariantsMap.containsKey(HippoStdNodeType.UNPUBLISHED)) {
                translationVariantNode = documentVariantsMap.get(HippoStdNodeType.UNPUBLISHED);
            } else if (documentVariantsMap.containsKey(HippoStdNodeType.PUBLISHED)) {
                translationVariantNode = documentVariantsMap.get(HippoStdNodeType.PUBLISHED);
            }

            if (translationVariantNode == null) {
                throw new IllegalStateException("No available unpublished or published variant in document at '"
                        + sourceDocumentLocation + "'.");
            }

            TranslationWorkflow documentTranslationWorkflow = getDocumentTranslationWorkflow(translationVariantNode);
            Document translatedDocument = documentTranslationWorkflow.addTranslation(targetLanguage,
                    targetDocumentNodeName);
            Node translatedDocumentHandleNode = HippoWorkflowUtils
                    .getHippoDocumentHandle(translatedDocument.getNode(getSession()));
            targetDocumentLocation = translatedDocumentHandleNode.getPath();
        } catch (RepositoryException | WorkflowException | RemoteException e) {
            log.error("Failed to translate document at '{}' to '{}' in '{}'.", sourceDocumentLocation,
                    targetDocumentNodeName, targetLanguage, e);
            throw new DocumentManagerException("Failed to add translated document of '" + sourceDocumentLocation + "' to '"
                    + targetDocumentNodeName + "' in '" + targetLanguage + "'. " + e);
        }

        return targetDocumentLocation;
    }

    protected Session getSession() {
        return session;
    }

    protected DocumentWorkflow getDocumentWorkflow(final Node documentHandleNode) throws RepositoryException {
        return (DocumentWorkflow) HippoWorkflowUtils.getHippoWorkflow(getSession(), getDocumentWorkflowCategory(),
                documentHandleNode);
    }

    protected TranslationWorkflow getFolderTranslationWorkflow(final Node folderNode) throws RepositoryException {
        return (TranslationWorkflow) HippoWorkflowUtils.getHippoWorkflow(getSession(), "translation", folderNode);
    }

    protected TranslationWorkflow getDocumentTranslationWorkflow(final Node documentVariantNode)
            throws RepositoryException {
        return (TranslationWorkflow) HippoWorkflowUtils.getHippoWorkflow(getSession(), "translation",
                documentVariantNode);
    }
}
