/*
 * Copyright 2024 Bloomreach B.V. (https://www.bloomreach.com)
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for ZipCompressUtils to verify proper Unicode filename handling,
 * especially for Cyrillic and other non-ASCII characters.
 * JIRA: FORGE-448 - Cyrillic characters in filenames not encoded properly
 */
public class ZipCompressUtilsTest {

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("zip-test").toFile();
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    /**
     * Test that Cyrillic characters in entry names are properly preserved in ZIP.
     * This is the core issue from FORGE-448.
     */
    @Test
    public void testCyrillicCharactersInEntryName() throws IOException {
        String cyrillicContent = "Test content for Cyrillic filename";
        String cyrillicEntryName = "документ/тест.txt";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            ZipCompressUtils.addEntryToZip(cyrillicEntryName, cyrillicContent, "UTF-8", zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        assertNotNull("ZIP content should not be null", zipBytes);
        assertTrue("ZIP content should not be empty", zipBytes.length > 0);

        // Verify the entry can be read back with correct name
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull("ZIP should contain an entry", entry);
            assertEquals("Entry name should match original Cyrillic name", cyrillicEntryName, entry.getName());

            // Verify content
            byte[] buffer = new byte[1024];
            int bytesRead = zis.read(buffer);
            String readContent = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            assertEquals("Content should match", cyrillicContent, readContent);
        } finally {
            zis.close();
        }
    }

    /**
     * Test that various Unicode scripts are properly preserved (Greek, Arabic, CJK, etc.)
     */
    @Test
    public void testVariousUnicodeScriptsInEntryName() throws IOException {
        String[] testCases = {
            "ελληνικά/αρχείο.txt",        // Greek
            "العربية/ملف.txt",              // Arabic
            "中文/文件.txt",                 // Simplified Chinese
            "日本語/ファイル.txt",           // Japanese
            "한글/파일.txt"                 // Korean
        };

        for (String entryName : testCases) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
            zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

            try {
                String content = "Content for " + entryName;
                ZipCompressUtils.addEntryToZip(entryName, content, "UTF-8", zipOutput);
            } finally {
                zipOutput.finish();
                zipOutput.close();
            }

            byte[] zipBytes = baos.toByteArray();
            ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
            try {
                ZipEntry entry = zis.getNextEntry();
                assertNotNull("ZIP should contain an entry for: " + entryName, entry);
                assertEquals("Entry name should match original: " + entryName, entryName, entry.getName());
            } finally {
                zis.close();
            }
        }
    }

    /**
     * Test binary content with Cyrillic entry names
     */
    @Test
    public void testBinaryContentWithCyrillicName() throws IOException {
        byte[] binaryContent = new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, 0x01, 0x02, 0x03};
        String cyrillicEntryName = "бинарный/файл.bin";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            ZipCompressUtils.addEntryToZip(cyrillicEntryName, binaryContent, zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull("ZIP should contain an entry", entry);
            assertEquals("Entry name should match", cyrillicEntryName, entry.getName());

            byte[] readContent = new byte[binaryContent.length];
            int bytesRead = zis.read(readContent);
            assertEquals("All bytes should be read", binaryContent.length, bytesRead);
            assertArrayEquals("Binary content should match", binaryContent, readContent);
        } finally {
            zis.close();
        }
    }

    /**
     * Test file entries from folder with Cyrillic file names
     */
    @Test
    public void testFileEntriesFromFolderWithCyrillicNames() throws IOException {
        // Create test folder structure with Cyrillic names
        File testFolder = new File(tempDir, "тестовая_папка");
        testFolder.mkdir();

        File file1 = new File(testFolder, "документ1.txt");
        Files.write(file1.toPath(), "Содержание 1".getBytes(StandardCharsets.UTF_8));

        File file2 = new File(testFolder, "документ2.txt");
        Files.write(file2.toPath(), "Содержание 2".getBytes(StandardCharsets.UTF_8));

        File subFolder = new File(testFolder, "подпапка");
        subFolder.mkdir();
        File file3 = new File(subFolder, "документ3.txt");
        Files.write(file3.toPath(), "Содержание 3".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            ZipCompressUtils.addFileEntriesInFolderToZip(testFolder, "", zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        assertTrue("ZIP should contain content", zipBytes.length > 0);

        // Verify all entries are present with correct names
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            int entryCount = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                // Entry names should not contain question marks (sign of encoding failure)
                assertFalse("Entry name should not contain question marks: " + entry.getName(),
                        entry.getName().contains("?"));
            }
            assertTrue("ZIP should contain at least 3 entries", entryCount >= 3);
        } finally {
            zis.close();
        }
    }

    /**
     * Test mixed ASCII and Cyrillic file names
     */
    @Test
    public void testMixedAsciiAndCyrillicNames() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            // Add various entries with mixed naming
            ZipCompressUtils.addEntryToZip("english.txt", "English content", "UTF-8", zipOutput);
            ZipCompressUtils.addEntryToZip("русский.txt", "Русский контент", "UTF-8", zipOutput);
            ZipCompressUtils.addEntryToZip("path/to/file.txt", "Nested content", "UTF-8", zipOutput);
            ZipCompressUtils.addEntryToZip("путь/к/файлу.txt", "Вложенный контент", "UTF-8", zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            int entryCount = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertFalse("Entry name should not contain question marks: " + entry.getName(),
                        entry.getName().contains("?"));
            }
            assertEquals("Should have 4 entries", 4, entryCount);
        } finally {
            zis.close();
        }
    }

    /**
     * Test that content encoding is preserved as UTF-8
     */
    @Test
    public void testUtf8ContentEncoding() throws IOException {
        String cyrillicContent = "Тестовое содержимое с кириллицей";
        String entryName = "файл.txt";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            ZipCompressUtils.addEntryToZip(entryName, cyrillicContent, "UTF-8", zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull("ZIP should contain an entry", entry);

            byte[] buffer = new byte[1024];
            int bytesRead = zis.read(buffer);
            String readContent = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            assertEquals("Content should match original Cyrillic text", cyrillicContent, readContent);
        } finally {
            zis.close();
        }
    }

    /**
     * Test offset-based entry addition with Cyrillic names
     */
    @Test
    public void testOffsetBasedEntryWithCyrillicName() throws IOException {
        byte[] fullContent = "Beginning content Middle content End content".getBytes(StandardCharsets.UTF_8);
        String cyrillicEntryName = "извлеченный_файл.txt";
        int offset = 10;
        int length = 14;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(baos);
        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

        try {
            ZipCompressUtils.addEntryToZip(cyrillicEntryName, fullContent, offset, length, zipOutput);
        } finally {
            zipOutput.finish();
            zipOutput.close();
        }

        byte[] zipBytes = baos.toByteArray();
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
        try {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull("ZIP should contain an entry", entry);
            assertEquals("Entry name should match", cyrillicEntryName, entry.getName());

            byte[] buffer = new byte[1024];
            int bytesRead = zis.read(buffer);
            byte[] expectedContent = new byte[length];
            System.arraycopy(fullContent, offset, expectedContent, 0, length);
            byte[] actualContent = new byte[bytesRead];
            System.arraycopy(buffer, 0, actualContent, 0, bytesRead);
            assertArrayEquals("Content should match extracted portion", expectedContent, actualContent);
        } finally {
            zis.close();
        }
    }
}
