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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

/**
 * ContentFileObjectUtils.
 */
public class ContentFileObjectUtils {

    private ContentFileObjectUtils() {
    }

    public static FileObject createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        return VFS.getManager().toFileObject(file);
    }

    public static FileObject createTempFile(String prefix, String suffix, FileObject directory) throws IOException {
        File file = File.createTempFile(prefix, suffix, toFile(directory));
        return toFileObject(file);
    }

    public static File toFile(FileObject fileObject) throws IOException {
        try {
            return new File(fileObject.getURL().toURI());
        } catch (FileSystemException | URISyntaxException e) {
            throw new IOException(e.toString(), e);
        }
    }

    public static FileObject toFileObject(File file) throws IOException {
        return VFS.getManager().toFileObject(file);
    }
}
