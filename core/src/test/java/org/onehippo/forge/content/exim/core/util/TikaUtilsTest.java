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
package org.onehippo.forge.content.exim.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TikaUtils} PDF parsing functionality.
 */
class TikaUtilsTest {

    private static final String TEST_PDF_CONTENT = "Hello World from TikaUtils Test";
    private static final String MULTILINE_CONTENT_LINE1 = "First line of content";
    private static final String MULTILINE_CONTENT_LINE2 = "Second line of content";

    private static byte[] simplePdfBytes;
    private static byte[] multilinePdfBytes;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void createTestPdfs() throws IOException {
        simplePdfBytes = createPdfWithText(TEST_PDF_CONTENT);
        multilinePdfBytes = createPdfWithText(MULTILINE_CONTENT_LINE1 + "\n" + MULTILINE_CONTENT_LINE2);
    }

    // ========================================================================
    // parsePdfToString(InputStream) tests
    // ========================================================================

    @Test
    void parsePdfToString_withInputStream_extractsText() throws Exception {
        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream);

            assertNotNull(result);
            assertTrue(result.contains("Hello World"), "Should extract text content");
        }
    }

    @Test
    void parsePdfToString_withInputStream_extractsMultilineText() throws Exception {
        try (InputStream pdfStream = new ByteArrayInputStream(multilinePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream);

            assertNotNull(result);
            assertTrue(result.contains("First line"), "Should contain first line");
            assertTrue(result.contains("Second line"), "Should contain second line");
        }
    }

    @Test
    void parsePdfToString_withNullInputStream_throwsException() {
        assertThrows(NullPointerException.class, () -> {
            TikaUtils.parsePdfToString((InputStream) null);
        });
    }

    @Test
    void parsePdfToString_withEmptyInputStream_throwsIOException() throws Exception {
        try (InputStream emptyStream = new ByteArrayInputStream(new byte[0])) {
            // Empty/invalid PDF should throw IOException
            assertThrows(IOException.class, () -> {
                TikaUtils.parsePdfToString(emptyStream);
            });
        }
    }

    @Test
    void parsePdfToString_withInvalidPdfData_throwsIOException() {
        byte[] invalidPdf = "This is not a PDF".getBytes();
        InputStream invalidStream = new ByteArrayInputStream(invalidPdf);
        assertThrows(IOException.class, () -> {
            TikaUtils.parsePdfToString(invalidStream);
        });
    }

    // ========================================================================
    // parsePdfToString(InputStream, Metadata) tests
    // ========================================================================

    @Test
    void parsePdfToString_withMetadata_extractsTextAndPopulatesMetadata() throws Exception {
        Metadata metadata = new Metadata();

        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream, metadata);

            assertNotNull(result);
            assertTrue(result.contains("Hello World"), "Should extract text content");
            // Metadata should be populated (at minimum content-type)
            assertNotNull(metadata.get("Content-Type"));
        }
    }

    @Test
    void parsePdfToString_withNullMetadata_throwsNullPointerException() throws Exception {
        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            assertThrows(NullPointerException.class, () -> {
                TikaUtils.parsePdfToString(pdfStream, null);
            });
        }
    }

    // ========================================================================
    // parsePdfToString(InputStream, Metadata, int maxLength) tests
    // ========================================================================

    @Test
    void parsePdfToString_withMaxLength_truncatesOutput() throws Exception {
        Metadata metadata = new Metadata();
        int maxLength = 10;

        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream, metadata, maxLength);

            assertNotNull(result);
            assertTrue(result.length() <= maxLength,
                "Result length " + result.length() + " should be <= " + maxLength);
        }
    }

    @Test
    void parsePdfToString_withLargeMaxLength_returnsFullContent() throws Exception {
        Metadata metadata = new Metadata();
        int maxLength = 10000;

        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream, metadata, maxLength);

            assertNotNull(result);
            assertTrue(result.contains("Hello World"), "Should contain full text");
        }
    }

    @Test
    void parsePdfToString_withZeroMaxLength_returnsEmptyString() throws Exception {
        Metadata metadata = new Metadata();

        try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
            String result = TikaUtils.parsePdfToString(pdfStream, metadata, 0);

            assertNotNull(result);
            assertEquals("", result, "Zero max length should return empty string");
        }
    }

    // ========================================================================
    // parsePdfToString(File) tests
    // ========================================================================

    @Test
    void parsePdfToString_withFile_extractsText() throws Exception {
        File pdfFile = tempDir.resolve("test.pdf").toFile();
        Files.write(pdfFile.toPath(), simplePdfBytes);

        String result = TikaUtils.parsePdfToString(pdfFile);

        assertNotNull(result);
        assertTrue(result.contains("Hello World"), "Should extract text from file");
    }

    @Test
    void parsePdfToString_withNonExistentFile_throwsIOException() {
        File nonExistentFile = new File("/nonexistent/path/to/file.pdf");

        assertThrows(IOException.class, () -> {
            TikaUtils.parsePdfToString(nonExistentFile);
        });
    }

    @Test
    void parsePdfToString_withNullFile_throwsException() {
        assertThrows(NullPointerException.class, () -> {
            TikaUtils.parsePdfToString((File) null);
        });
    }

    // ========================================================================
    // parsePdfToString(URL) tests
    // ========================================================================

    @Test
    void parsePdfToString_withFileUrl_extractsText() throws Exception {
        File pdfFile = tempDir.resolve("test-url.pdf").toFile();
        Files.write(pdfFile.toPath(), simplePdfBytes);
        URL pdfUrl = pdfFile.toURI().toURL();

        String result = TikaUtils.parsePdfToString(pdfUrl);

        assertNotNull(result);
        assertTrue(result.contains("Hello World"), "Should extract text from URL");
    }

    @Test
    void parsePdfToString_withNullUrl_throwsException() {
        assertThrows(NullPointerException.class, () -> {
            TikaUtils.parsePdfToString((URL) null);
        });
    }

    // ========================================================================
    // Singleton/Thread-safety tests
    // ========================================================================

    @Test
    void parsePdfToString_multipleCalls_reusesTikaInstance() throws Exception {
        // Call multiple times to ensure the singleton pattern works
        for (int i = 0; i < 5; i++) {
            try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
                String result = TikaUtils.parsePdfToString(pdfStream);
                assertNotNull(result);
                assertTrue(result.contains("Hello World"));
            }
        }
    }

    @Test
    void parsePdfToString_concurrentCalls_threadSafe() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        Throwable[] exceptions = new Throwable[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try (InputStream pdfStream = new ByteArrayInputStream(simplePdfBytes)) {
                    String result = TikaUtils.parsePdfToString(pdfStream);
                    if (!result.contains("Hello World")) {
                        exceptions[index] = new AssertionError("Missing expected content");
                    }
                } catch (Exception e) {
                    exceptions[index] = e;
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " failed: " + exceptions[i].getMessage());
            }
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private static byte[] createPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);

                // Handle multiline text
                String[] lines = text.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) {
                        contentStream.newLineAtOffset(0, -15);
                    }
                    contentStream.showText(lines[i]);
                }

                contentStream.endText();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }
}
