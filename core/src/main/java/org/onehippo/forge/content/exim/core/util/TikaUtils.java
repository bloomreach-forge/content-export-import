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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.detect.NameDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.pdf.PDFParser;

/**
 * Apache Tika utilities.
 */
public final class TikaUtils {

    private static final Object lock = new Object();

    private static volatile Tika tikaForPdf;

    private TikaUtils() {
    }

    /**
     * Parses the given document and returns the extracted text content.
     * @param pdfStream PDF input stream
     * @return extracted text content
     * @throws IOException if IO exception occurs
     * @throws TikaException if Tika exception occurs
     */
    public static String parsePdfToString(final InputStream pdfStream)
            throws IOException, TikaException {
        return getTikaForPdf().parseToString(pdfStream);
    }

    /**
     * Parses the given document and returns the extracted text content.
     * @param pdfStream PDF input stream
     * @param metadata document metadata
     * @return extracted text content
     * @throws IOException if IO exception occurs
     * @throws TikaException if Tika exception occurs
     */
    public static String parsePdfToString(final InputStream pdfStream, final Metadata metadata)
            throws IOException, TikaException {
        return getTikaForPdf().parseToString(pdfStream, metadata);
    }

    /**
     * Parses the given document and returns the extracted text content.
     * @param pdfStream PDF input stream
     * @param metadata document metadata
     * @param maxLength maximum length of the returned string
     * @return extracted text content
     * @throws IOException if IO exception occurs
     * @throws TikaException if Tika exception occurs
     */
    public static String parsePdfToString(final InputStream pdfStream, final Metadata metadata, final int maxLength)
            throws IOException, TikaException {
        return getTikaForPdf().parseToString(pdfStream, metadata, maxLength);
    }

    /**
     * Parses the given document and returns the extracted text content.
     * @param pdfFile PDF file
     * @return extracted text content
     * @throws IOException if IO exception occurs
     * @throws TikaException if Tika exception occurs
     */
    public static String parsePdfToString(final File pdfFile) throws IOException, TikaException {
        return getTikaForPdf().parseToString(pdfFile);
    }

    /**
     * Parses the given document and returns the extracted text content.
     * @param pdfURL PDF resource URL
     * @return extracted text content
     * @throws IOException if IO exception occurs
     * @throws TikaException if Tika exception occurs
     */
    public static String parsePdfToString(final URL pdfURL) throws IOException, TikaException {
        return getTikaForPdf().parseToString(pdfURL);
    }

    private static Tika getTikaForPdf() {
        Tika tika = tikaForPdf;

        if (tika == null) {
            synchronized (lock) {
                tika = tikaForPdf;

                if (tika == null) {
                    Map<Pattern, MediaType> patterns = new HashMap<Pattern, MediaType>();
                    patterns.put(Pattern.compile(".*\\.pdf", Pattern.CASE_INSENSITIVE), MediaType.application("pdf"));
                    NameDetector detector = new NameDetector(patterns);
                    tikaForPdf = tika = new Tika(detector, new PDFParser());
                    return tika;
                }
            }
        }

        return tika;
    }

}
