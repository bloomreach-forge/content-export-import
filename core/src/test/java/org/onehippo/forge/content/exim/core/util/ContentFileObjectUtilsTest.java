/*
 * Copyright 2025 Bloomreach B.V. (https://www.bloomreach.com)
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

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentFileObjectUtilsTest {

    @Test
    void createTempFile_returnsNonNullFileObject() throws Exception {
        FileObject fo = ContentFileObjectUtils.createTempFile("exim-test-", ".tmp");
        assertNotNull(fo);
        assertTrue(fo.exists());
        fo.delete();
    }

    @Test
    void createTempFile_withDirectory_createsUnderDirectory() throws Exception {
        FileObject dir = ContentFileObjectUtils.toFileObject(new File(System.getProperty("java.io.tmpdir")));
        FileObject fo = ContentFileObjectUtils.createTempFile("exim-dir-test-", ".tmp", dir);
        assertNotNull(fo);
        assertTrue(fo.exists());
        fo.delete();
    }

    @Test
    void toFile_roundtrip_returnsSameAbsolutePath() throws Exception {
        File original = File.createTempFile("exim-roundtrip-", ".tmp");
        original.deleteOnExit();
        FileObject fo = ContentFileObjectUtils.toFileObject(original);
        File restored = ContentFileObjectUtils.toFile(fo);
        assertTrue(restored.exists());
        // canonical paths must agree
        assertTrue(original.getCanonicalPath().equals(restored.getCanonicalPath()));
    }

    @Test
    void toFileObject_fromFile_returnsFileObject() throws Exception {
        File f = File.createTempFile("exim-fo-", ".tmp");
        f.deleteOnExit();
        FileObject fo = ContentFileObjectUtils.toFileObject(f);
        assertNotNull(fo);
        assertTrue(fo.exists());
    }
}
