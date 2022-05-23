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
package org.onehippo.forge.content.exim.core;

import javax.jcr.Node;

import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * <code>ContentMigrationTask</code> to import {@link ContentNode} objects and create or update documents.
 */
public interface DocumentVariantImportTask extends ContentMigrationTask {

    /**
     * Creates or updates the document as primary node type of {@code primaryTypeName}
     * at the document handle node path ({@code documentHandlePath})
     * with a localized document name ({@code localizedName}) for the specific {@code locale}.
     *
     * @param contentNode {@link ContentNode} instance as a source data to create or update a document
     * @param primaryTypeName primary node type name of the document to create
     * @param documentLocation document handle node path where the document should be created or updated
     * @param locale locale name for the {@code localizedName}
     * @param localizedName localized document name
     *
     * @return the document handle path where the document was created or updated by this operation
     *
     * @throws ContentMigrationException if creation or updating fails
     */
    String createOrUpdateDocumentFromVariantContentNode(ContentNode contentNode, String primaryTypeName,
            String documentLocation, String locale, String localizedName) throws ContentMigrationException;

    /**
     * Updates the document given by {@code documentHandle} from the POJO {@code contentNode}.

     * @param contentNode {@link ContentNode} instance as a source data to create or update a document
     * @param documentNode document handle JCR node that is to be updated, or a document JCR node that has a handle as parent.
     *
     * @return the document handle node of the document updated by this operation
     *
     * @throws IllegalArgumentException if documentNode is null, not a handle nor a document
     * @throws ContentMigrationException if updating fails
     */
    Node updateDocumentFromVariantContentNode(ContentNode contentNode, Node documentNode) throws ContentMigrationException;

}
