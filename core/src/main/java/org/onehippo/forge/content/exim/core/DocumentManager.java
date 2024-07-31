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
package org.onehippo.forge.content.exim.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;

/**
 * Hippo CMS Document/Folder Workflow manager.
 */
public interface DocumentManager {

    /**
     * Returns the logger used by DocumentManager.
     * @return the logger used by DocumentManager
     */
    Logger getLogger();

    /**
     * Sets a logger to DocumentManager.
     * @param logger the logger to be used by DocumentManager
     */
    void setLogger(Logger logger);

    /**
     * Returns the JCR session.
     * @return the JCR session
     */
    Session getSession();

    /**
     * Returns true if a document exists at {@code documentLocation}.
     * @param documentLocation document handle node path
     * @return true if a document exists at {@code documentLocation}
     * @throws DocumentManagerException if fails to process
     */
    boolean documentExists(String documentLocation) throws DocumentManagerException;

    /**
     * Returns the physical document handle node path for the logical document location.
     * Returns null if document doesn't exist at the location, without any exception,
     * unlike {@link #getExistingDocumentHandleNode(String)}.
     * @param documentLocation logical document location
     * @return the physical document handle node path for the logical document location
     * @throws DocumentManagerException if fails to process
     */
    String getExistingDocumentPath(String documentLocation) throws DocumentManagerException;

    /**
     * Returns the physical document handle node for the logical document location.
     * Throws a {@link DocumentManagerNotFoundException} if document doesn't exist at the location,
     * unlike {@link #getExistingDocumentPath(String)}.
     * @param documentLocation logical document location
     * @return the physical document handle node for the logical document location
     * @throws DocumentManagerNotFoundException if cannot find a document
     * @throws RepositoryException if any repository exception occurs
     */
    Node getExistingDocumentHandleNode(String documentLocation) throws DocumentManagerNotFoundException, RepositoryException;

    /**
     * Returns true if a folder exists at {@code folderLocation}.
     * @param folderLocation folder node path
     * @return true if a folder exists at {@code folderLocation}
     * @throws DocumentManagerException if fails to process
     */
    boolean folderExists(String folderLocation) throws DocumentManagerException;

    /**
     * Returns the physical folder node path for the logical folder location.
     * Returns null if folder doesn't exist at the location, without any exception,
     * unlike {@link #getExistingFolderNode(String)}.
     * @param folderLocation logical folder location
     * @return the physical folder node path for the logical folder location
     * @throws DocumentManagerException if fails to process
     */
    String getExistingFolderPath(String folderLocation) throws DocumentManagerException;

    /**
     * Returns the physical folder node for the logical folder location.
     * Throws a {@link DocumentManagerNotFoundException} if folder doesn't exist at the location,
     * unlike {@link #getExistingDocumentPath(String)}.
     * @param folderLocation logical folder location
     * @return the physical folder node for the logical folder location
     * @throws DocumentManagerException if cannot find a folder
     * @throws RepositoryException if any repository exception occurs
     */
    Node getExistingFolderNode(String folderLocation) throws DocumentManagerNotFoundException, RepositoryException;

    /**
     * Creates a document in the specific {@code folderLocation}.
     * @param folderLocation destination folder path
     * @param primaryTypeName primary node type name of document to be created
     * @param nodeName document node name
     * @param locale locale for the document display name. e.g, "en"
     * @param localizedName localized document name associated with the {@code locale}
     * @return created document handle path
     * @throws DocumentManagerException if fails to process
     */
    String createDocument(String folderLocation, String primaryTypeName, String nodeName, String locale,
            String localizedName) throws DocumentManagerException;

    /**
     * Obtains an editable draft variant {@link Document} under the given document handle path ({@code documentLocation}).
     * @param documentLocation document handle path
     * @return a {@link Document} instance if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document obtainEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Obtains an editable draft variant {@link Document} of the given document handle node ({@code documentHandleNode}).
     *
     * @param documentHandleNode document handle node
     * @return a {@link Document} instance if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document obtainEditableDocument(Node documentHandleNode) throws DocumentManagerException;

    /**
     * Update editable {@link Document} instance ({@code editableDocument}) by the content of the given {@code sourceContentNode}.
     * @param editableDocument {@link Document} instance to edit
     * @param sourceContentNode source content node
     * @throws DocumentManagerException if fails to process
     */
    void updateEditableDocument(Document editableDocument, ContentNode sourceContentNode)
            throws DocumentManagerException;

    /**
     * Discards the draft variant which is currently being edited.
     * @param documentLocation document handle path
     * @return {@link Document} instance discarded if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document disposeEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Discards the draft variant which is currently being edited.
     * @param editableDocument document object
     * @return {@link Document} instance discarded if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document disposeEditableDocument(Document editableDocument) throws DocumentManagerException;

    /**
     * Commits the draft variant which is currently being edited.
     * @param documentLocation document handle path
     * @return {@link Document} instance committed if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document commitEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Commits the draft variant which is currently being edited.

     * @param editableDocument document object
     * @return {@link Document} instance committed if the operation was successful
     * @throws DocumentManagerException if fails to process
     */
    Document commitEditableDocument(Document editableDocument) throws DocumentManagerException;


    /**
     * Publishes the document at the given document handle path ({@code documentLocation}).
     * @param documentLocation document handle path
     * @return true if the operation was successful, false otherwise
     * @throws DocumentManagerException if fails to process
     */
    boolean publishDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Takes offline the document at the given document handle path ({@code documentLocation}).
     * @param documentLocation document handle path
     * @return true if the operation was successful, false otherwise
     * @throws DocumentManagerException if fails to process
     */
    boolean depublishDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Deletes a document at at the given document handle path ({@code documentLocation}).
     * @param documentLocation document handle path
     * @throws DocumentManagerException if fails to process
     */
    void deleteDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Copies a document at the source document handle path ({@code sourceDocumentLocation}) to
     * the target folder path ({@code targetFolderLocation}) with the given document node name ({@code targetDocumentName}).
     * @param sourceDocumentLocation source document handle path
     * @param targetFolderLocation target folder path
     * @param targetDocumentName target document handle node name
     * @return the copied target document handle path
     * @throws DocumentManagerException if fails to process
     */
    String copyDocument(String sourceDocumentLocation, String targetFolderLocation, String targetDocumentName)
            throws DocumentManagerException;

    /**
     * Translates a folder at the folder path ({@code sourceFolderLocation}) to {@code name} in {@code language}.
     * @param sourceFolderLocation source folder path
     * @param language target language to translate to
     * @param name target folder node name
     * @return the translated target folder {@code Document} instance
     * @throws DocumentManagerException if fails to process
     */
    Document translateFolder(String sourceFolderLocation, String language, String name) throws DocumentManagerException;

    /**
     * Translates a document at the document handle path ({@code sourceDocumentLocation}) to {@code name} in {@code language}.
     * @param sourceDocumentLocation source document handle path
     * @param language target language to translate to
     * @param name target document handle node name
     * @return the translated target document {@code Document} instance
     * @throws DocumentManagerException if fails to process
     */
    Document translateDocument(String sourceDocumentLocation, String language, String name)
            throws DocumentManagerException;

    /**
     * Returns a document workflow on {@code documentHandleNode}.
     * @param documentHandleNode document handle node
     * @return a document workflow on {@code documentHandleNode}
     * @throws RepositoryException if unexpected repository exception occurs
     */
    public DocumentWorkflow getDocumentWorkflow(final Node documentHandleNode) throws RepositoryException;

}
