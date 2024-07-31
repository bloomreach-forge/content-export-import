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

import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * <code>ContentMigrationTask</code> to import binary (gallery/asset) data from {@link ContentNode} objects.
 */
public interface BinaryImportTask extends ContentMigrationTask {

    String createOrUpdateBinaryFolder(String folderLocation, String primaryTypeName, String [] folderTypes,
            String [] galleryTypes) throws ContentMigrationException;

    /**
     * Creates an imageset (e.g, <code>hippogallery:imageset</code>) content or
     * an assetset (e.g, <code>hippogallery:exampleAssetSet</code>) content from a {@link ContentNode} object.
     * @param contentNode a {@link ContentNode} object containing an imageset (e.g, <code>hippogallery:imageset</code>) content
     *                    or an assetset (e.g, <code>hippogallery:exampleAssetSet</code>) content
     * @param primaryTypeName primary node type of binary content.
     *                        e.g, <code>hippogallery:imageset</code>, <code>hippogallery:exampleAssetSet</code>, etc.
     * @param folderPath folder node path where this binary content should be created or updated
     * @param name binary content node name
     * @return created or updated binary handle node path
     * @throws ContentMigrationException if importing fails
     */
    String createOrUpdateBinaryFromContentNode(ContentNode contentNode, String primaryTypeName, String folderPath,
            String name) throws ContentMigrationException;

}
