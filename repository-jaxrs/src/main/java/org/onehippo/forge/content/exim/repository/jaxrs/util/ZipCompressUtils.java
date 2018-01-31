/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.content.exim.repository.jaxrs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ZipCompressUtils {

    private ZipCompressUtils() {
    }

    public static void addEntryToZip(String entryName, String content, String charsetName,
            ZipArchiveOutputStream zipOutput) throws IOException {
        byte[] bytes;

        if (StringUtils.isBlank(charsetName)) {
            bytes = content.getBytes();
        } else {
            bytes = content.getBytes(charsetName);
        }

        addEntryToZip(entryName, bytes, 0, bytes.length, zipOutput);
    }

    public static void addEntryToZip(String entryName, byte[] bytes, int offset, int length,
            ZipArchiveOutputStream zipOutput) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        entry.setSize(length);

        try {
            zipOutput.putArchiveEntry(entry);
            zipOutput.write(bytes, offset, length);
        } finally {
            zipOutput.closeArchiveEntry();
        }
    }

    public static void addFileEntriesInFolderToZip(File baseFolder, String prefix, ZipArchiveOutputStream zipOutput)
            throws IOException {
        for (File file : baseFolder.listFiles()) {
            String entryName = (StringUtils.isEmpty(prefix)) ? file.getName() : (prefix + "/" + file.getName());

            if (file.isFile()) {
                ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
                entry.setSize(file.length());
                InputStream input = null;

                try {
                    zipOutput.putArchiveEntry(entry);
                    input = new FileInputStream(file);
                    IOUtils.copyLarge(input, zipOutput);
                } finally {
                    IOUtils.closeQuietly(input);
                    zipOutput.closeArchiveEntry();
                }
            } else {
                addFileEntriesInFolderToZip(file, entryName, zipOutput);
            }
        }
    }
}
