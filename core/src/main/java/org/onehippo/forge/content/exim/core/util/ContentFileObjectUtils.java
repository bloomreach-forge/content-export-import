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
package org.onehippo.forge.content.exim.core.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

/**
 * ContentFileObjectUtils providing utilities for {@link FileObject} and {@link File}.
 */
public class ContentFileObjectUtils {

    private ContentFileObjectUtils() {
    }

    /**
     * Create a temporary file by {@code prefix} and {@code suffix} and returns it as a {@link FileObject}.
     * @param prefix temporary file prefix
     * @param suffix temporary file suffix
     * @return a temporary file by {@code prefix} and {@code suffix} and returns it as a {@link FileObject}
     * @throws IOException if IOException occurs
     */
    public static FileObject createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        return VFS.getManager().toFileObject(file);
    }

    /**
     * Create a temporary file by {@code prefix} and {@code suffix} under {@code directory} and returns it as a {@link FileObject}.
     * @param prefix temporary file prefix
     * @param suffix temporary file suffix
     * @param directory base directory
     * @return a temporary file by {@code prefix} and {@code suffix} and returns it as a {@link FileObject}
     * @throws IOException if IOException occurs
     */
    public static FileObject createTempFile(String prefix, String suffix, FileObject directory) throws IOException {
        File file = File.createTempFile(prefix, suffix, toFile(directory));
        return toFileObject(file);
    }

    /**
     * Converts {@code fileObject} to a {@link File} instance and return it if {@code fileObject} is a local file.
     * @param fileObject {@link FileObject} instance
     * @return Converts {@code fileObject} to a {@link File} instance and return it if {@code fileObject} is a local file
     * @throws IOException if {@code fileObject} is not a local file or any IOException occurs
     */
    public static File toFile(FileObject fileObject) throws IOException {
        try {
            return new File(fileObject.getURL().toURI());
        } catch (FileSystemException | URISyntaxException e) {
            throw new IOException(e.toString(), e);
        }
    }

    /**
     * Creates a {@link FileObject} from the give local {@link File} object ({@code file}).
     * @param file a local {@link File} object
     * @return a {@link FileObject} from the give local {@link File} object ({@code file})
     * @throws IOException if any IOException occurs
     */
    public static FileObject toFileObject(File file) throws IOException {
        return VFS.getManager().toFileObject(file);
    }
}
