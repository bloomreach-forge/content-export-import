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

import org.apache.commons.lang.StringUtils;

/**
 * Content Node Path related utilities.
 */
public class ContentPathUtils {

    private ContentPathUtils() {
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
