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
package org.onehippo.forge.content.exim.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Content Node Path related utilities.
 */
public class ContentPathUtils {

    private static final Pattern INDEX_NOTATION_PATTERN = Pattern.compile("\\[\\d+\\](\\/|$)");

    private static final Pattern HIPPO_CONTENT_PATH_PREFIX_PATTERN =
            Pattern.compile("^\\/content\\/(documents|gallery|assets)\\/[^\\\\]+");

    private ContentPathUtils() {
    }

    /**
     * Removes SNS (Same Name Sibling) index notation in the given {@code nodePath}.
     * @param nodePath node path
     * @return a node path without SNS (Same Name Sibling) index notation
     */
    public static String removeIndexNotationInNodePath(final String nodePath) {
        if (nodePath == null) {
            return nodePath;
        }

        final Matcher matcher = INDEX_NOTATION_PATTERN.matcher(nodePath);
        return matcher.replaceAll("$1");
    }

    /**
     * Returns encoded node path where each node name in the {@code nodePath} is encoded
     * by using Hippo CMS Default URI Encoding strategy.
     * @param nodePath node path
     * @return encoded node path where each node name in the {@code nodePath} is encoded
     *         by using Hippo CMS Default URI Encoding strategy
     */
    public static String encodeNodePath(final String nodePath) {
        String [] nodeNames = StringUtils.splitPreserveAllTokens(nodePath, '/');

        if (nodeNames == null) {
            return null;
        }

        // If the nodePath starts with typical Hippo content node path like '/content/documents/MyHippoProject/...',
        // DO NOT encode the first three path segments
        // because the third path segment might be in upper-cases unlike descendant nodes.

        int begin = 0;
        final Matcher m = HIPPO_CONTENT_PATH_PREFIX_PATTERN.matcher(nodePath);
        if (m.lookingAt()) {
            begin = 4;
        }

        for (int i = begin; i < nodeNames.length; i++) {
            nodeNames[i] = HippoNodeUtils.getDefaultUriEncoding().encode(nodeNames[i]);
        }

        return StringUtils.join(nodeNames, '/');
    }

    /**
     * Splits the given {@code contentLocation} to an array which consists of a folder path and the node name.
     * @param contentLocation content node path
     * @return an array which consists of a folder path and the node name
     */
    public static String [] splitToFolderPathAndName(final String contentLocation) {
        String [] folderPathAndName = new String [] { "", "" };
        int offset = StringUtils.lastIndexOf(contentLocation, '/');
        folderPathAndName[0] = StringUtils.substring(contentLocation, 0, offset);
        folderPathAndName[1] = StringUtils.substring(contentLocation, offset + 1);
        return folderPathAndName;
    }
}
