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
package org.onehippo.forge.content.exim.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentPathUtilsTest {

    @Test
    void testRemoveIndexNotationInNodePath() {
        String nodePath = "/a/b/c/d[2]";
        assertEquals("/a/b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "/a/b[2]/c/d[2]";
        assertEquals("/a/b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "a/b/c/d[2]";
        assertEquals("a/b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "a/b[2]/c/d[2]";
        assertEquals("a/b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "a/b/c/d[2]";
        assertEquals("a/b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "b[2]/c/d[2]";
        assertEquals("b/c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "c/d[2]";
        assertEquals("c/d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));

        nodePath = "d[2]";
        assertEquals("d", ContentPathUtils.removeIndexNotationInNodePath(nodePath));
    }

    @Test
    void testEncodeNodePath() {
        String nodePath = "/a/b/c";
        assertEquals("/a/b/c", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/a/b/C/d/E";
        assertEquals("/a/b/c/d/e", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/a/b/C And D";
        assertEquals("/a/b/c-and-d", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/a/b/C And D/";
        assertEquals("/a/b/c-and-d/", ContentPathUtils.encodeNodePath(nodePath));
    }

    @Test
    void testEncodeHippoContentNodePath() {
        String nodePath = "/content/documents/MyHippoProject/a/b/c";
        assertEquals("/content/documents/MyHippoProject/a/b/c", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/content/documents/MyHippoProject/a/b/C/d/E";
        assertEquals("/content/documents/MyHippoProject/a/b/c/d/e", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/content/documents/MyHippoProject/a/b/C And D";
        assertEquals("/content/documents/MyHippoProject/a/b/c-and-d", ContentPathUtils.encodeNodePath(nodePath));

        nodePath = "/content/documents/MyHippoProject/a/b/C And D/";
        assertEquals("/content/documents/MyHippoProject/a/b/c-and-d/", ContentPathUtils.encodeNodePath(nodePath));
    }
}
