/*
 * Copyright 2022 Bloomreach B.V. (https://www.bloomreach.com)
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

/**
 * ZIP Compressing Utilities.
 */
public class ZipCompressUtils {

    private ZipCompressUtils() {
    }

    /**
     * Add a ZIP entry to {@code zipOutput} with the given {@code entryName} and string {@code content} in
     * {@code charsetName} encoding.
     * @param entryName ZIP entry name
     * @param content string content of the ZIP entry.
     * @param charsetName charset name in encoding
     * @param zipOutput ZipArchiveOutputStream instance
     * @throws IOException if IO exception occurs
     */
    public static void addEntryToZip(String entryName, String content, String charsetName,
            ZipArchiveOutputStream zipOutput) throws IOException {
        byte[] bytes;

        if (StringUtils.isBlank(charsetName)) {
            bytes = content.getBytes();
        } else {
            bytes = content.getBytes(charsetName);
        }

        addEntryToZip(entryName, bytes, zipOutput);
    }

    /**
     * Add a ZIP entry to {@code zipOutput} with the given {@code entryName} and {@code bytes}.
     * @param entryName ZIP entry name
     * @param bytes the byte array to fill in for the ZIP entry
     * @param zipOutput ZipArchiveOutputStream instance
     * @throws IOException if IO exception occurs
     */
    public static void addEntryToZip(String entryName, byte[] bytes,
            ZipArchiveOutputStream zipOutput) throws IOException {
        addEntryToZip(entryName, bytes, 0, bytes.length, zipOutput);
    }

    /**
     * Add a ZIP entry to {@code zipOutput} with the given {@code entryName} and {@code bytes} starting from
     * {@code offset} in {@code length}.
     * @param entryName ZIP entry name
     * @param bytes the byte array to fill in for the ZIP entry
     * @param offset the starting offset index to read from the byte array
     * @param length the length to read from the byte array
     * @param zipOutput ZipArchiveOutputStream instance
     * @throws IOException if IO exception occurs
     */
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

    /**
     * Add ZIP entries to {@code zipOutput} by selecting all the descendant files under the {@code baseFolder},
     * starting with the ZIP entry name {@code prefix}.
     * @param baseFolder base folder to find child files underneath
     * @param prefix the prefix of ZIP entry name
     * @param zipOutput ZipArchiveOutputStream instance
     * @throws IOException if IO exception occurs
     */
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
