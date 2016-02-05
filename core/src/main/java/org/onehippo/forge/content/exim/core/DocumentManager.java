/*
 * Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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

/**
 * Hippo CMS Document/Folder Workflow manager.
 */
public interface DocumentManager {

    /**
     * Obtains an editable draft variant from the document handle location.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     */
    String obtainEditableDocument(String documentLocation);

    /**
     * Discards the draft variant currently being edited.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     */
    String disposeEditableDocument(String documentLocation);

    /**
     * Commits the draft variant currently being edited.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     */
    String commitEditableDocument(String documentLocation);

    /**
     * Takes offline the document.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     */
    boolean depublishDocument(String documentLocation);

    /**
     * Publishes the document.
     * @param documentLocation document handle location
     * @return true if the operation was successful, false otherwise
     */
    boolean publishDocument(String documentLocation);

    /**
     * Copies the {@code sourceDocumentLocation} to {@code targetFolderLocation} with the {@code targetDocumentName}.
     * @param sourceDocumentLocation source document handle location
     * @param targetFolderLocation target folder location
     * @param targetDocumentName target document handle node name
     * @return the copied target document handle location
     */
    String copyDocument(String sourceDocumentLocation, String targetFolderLocation, String targetDocumentName);

    /**
     * Translates the {@code sourceFolderLocation} to {@code language} with the {@code name}.
     * @param sourceFolderLocation source folder location
     * @param language target language to translate to
     * @param name target folder name
     * @return the translated target folder location
     */
    String translateFolder(String sourceFolderLocation, String language, String name);

    /**
     * Translates the {@code sourceDocumentLocation} to {@code language} with the {@code name}.
     * @param sourceDocumentLocation source document handle location
     * @param language target language to translate to
     * @param name target document name
     * @return the translated target document handle location
     */
    String translateDocument(String sourceDocumentLocation, String language, String name);

}
