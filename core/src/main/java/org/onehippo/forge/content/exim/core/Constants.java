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

import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Module-wise constants.
 */
public class Constants {

    /**
     * Meta property name to store the original document handle node path temporarily in a {@link ContentNode} object.
     * <P>
     * This meta property is useful when exporting a document content data into a {@link ContentNode} object
     * because the original document handle node path isn't available by default in the exported {@link ContentNode} object.
     * </P>
     * <P>
     * So, by storing this meta property temporarily in a {@link ContentNode} object, it can be used
     * when importing back from the {@link ContentNode} object later to determine where it should be imported.
     * Or, importing task modify this meta property to change the import target document handle location.
     * </P>
     */
    public static final String META_PROP_NODE_PATH = "jcr:path";

    /**
     * Meta property name to store a localized document name temporarily in a {@link ContentNode} object.
     * <P>
     * This meta property is useful when exporting a document variant content data into a {@link ContentNode} object
     * because the localized document name doesn't exist in the document variant node itself but it exists in a separate
     * translation node under the document handle node.
     * </P>
     * <P>
     * So, by storing this meta property temporarily in a {@link ContentNode} object, it can be used
     * when importing back from the {@link ContentNode} object later.
     * </P>
     */
    public static final String META_PROP_NODE_LOCALIZED_NAME = "jcr:localizedName";

    private Constants() {
    }
}
