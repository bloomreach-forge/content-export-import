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

import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * <code>ContentMigrationTask</code> to export document variant nodes to {@link ContentNode} objects.
 */
public interface DocumentVariantExportTask extends ContentMigrationTask {

    /**
     * Exports a document variant ({@link Document}), {@code document}, to a {@link ContentNode} object.
     * @param document a document variant ({@link Document})
     * @return a {@link ContentNode} object written with the {@code document} data
     * @throws ContentMigrationException if exporting fails
     */
    ContentNode exportVariantToContentNode(Document document) throws ContentMigrationException;

}
