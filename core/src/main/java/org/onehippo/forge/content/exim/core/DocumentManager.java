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
package org.onehippo.forge.content.exim.core;

import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Hippo CMS Document/Folder Workflow manager.
 */
public interface DocumentManager {

    /**
     * Returns the JCR session.
     * @return the JCR session
     */
    Session getSession();

    /**
     * Creates a document in the specific {@code folderLocation}
     * with given {@code templateCategory}, {@code prototype}, {@code name} and {@code displayName}.
     * @param folderLocation destination folder location
     * @param templateCategory template category name
     * @param prototype prototype name
     * @param nodeName document node name
     * @param locale locale name for the document display name
     * @param displayName document display name for the locale
     * @return created document location
     * @throws DocumentManagerException if fails to process.
     */
    String createDocument(String folderLocation, String templateCategory, String prototype, String nodeName, String locale, String displayName)
            throws DocumentManagerException;

    /**
     * Obtains an editable draft variant from the document handle location.
     * @param documentLocation document handle location
     * @return document if the operation was successful
     * @throws DocumentManagerException if fails to process.
     */
    Document obtainEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Update editable document (specified by {@code documentIdentifier}) by the content of the given {@code sourceContentNode}.
     * @param editableDocument document to edit
     * @param sourceContentNode source content node
     * @throws DocumentManagerException if fails to process.
     */
    void updateEditableDocument(Document editableDocument, ContentNode sourceContentNode)
            throws DocumentManagerException;

    /**
     * Discards the draft variant currently being edited.
     * @param documentLocation document handle location
     * @return document if the operation was successful
     * @throws DocumentManagerException if fails to process.
     */
    Document disposeEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Commits the draft variant currently being edited.
     * @param documentLocation document handle location
     * @return document if the operation was successful
     * @throws DocumentManagerException if fails to process.
     */
    Document commitEditableDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Publishes the document.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     * @throws DocumentManagerException if fails to process.
     */
    boolean publishDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Takes offline the document.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     * @throws DocumentManagerException if fails to process.
     */
    boolean depublishDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Deletes a document at {@code documentLocation}.
     * @param documentLocation document handle location
     * @throws DocumentManagerException if fails to process.
     */
    void deleteDocument(String documentLocation) throws DocumentManagerException;

    /**
     * Copies the {@code sourceDocumentLocation} to {@code targetFolderLocation} with the {@code targetDocumentName}.
     * @param sourceDocumentLocation source document handle location
     * @param targetFolderLocation target folder location
     * @param targetDocumentName target document handle node name
     * @return the copied target document handle location
     * @throws DocumentManagerException if fails to process.
     */
    String copyDocument(String sourceDocumentLocation, String targetFolderLocation, String targetDocumentName)
            throws DocumentManagerException;

    /**
     * Translates the {@code sourceFolderLocation} to {@code language} with the {@code name}.
     * @param sourceFolderLocation source folder location
     * @param language target language to translate to
     * @param name target folder name
     * @return the translated target folder document
     * @throws DocumentManagerException if fails to process.
     */
    Document translateFolder(String sourceFolderLocation, String language, String name) throws DocumentManagerException;

    /**
     * Translates the {@code sourceDocumentLocation} to {@code language} with the {@code name}.
     * @param sourceDocumentLocation source document handle location
     * @param language target language to translate to
     * @param name target document name
     * @return the translated target document
     * @throws DocumentManagerException if fails to process.
     */
    Document translateDocument(String sourceDocumentLocation, String language, String name)
            throws DocumentManagerException;

}
