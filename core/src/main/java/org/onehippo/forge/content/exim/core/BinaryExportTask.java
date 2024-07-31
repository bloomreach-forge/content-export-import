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

import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * <code>ContentMigrationTask</code> to export binary (gallery/asset) data to {@link ContentNode} objects.
 */
public interface BinaryExportTask extends ContentMigrationTask {

    /**
     * Exports an imageset (e.g, <code>hippogallery:imageset</code>) node or
     * an assetset (e.g, <code>hippogallery:exampleAssetSet</code>) node to a {@link ContentNode} object.
     * @param imageSetOrAssetSetNode an imageset (e.g, <code>hippogallery:imageset</code>) node or
     *                               an assetset (e.g, <code>hippogallery:exampleAssetSet</code>) node
     * @return an exported {@link ContentNode} object
     * @throws ContentMigrationException if exporting fails
     */
    ContentNode exportBinarySetToContentNode(Node imageSetOrAssetSetNode) throws ContentMigrationException;

}
