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
package org.onehippo.forge.content.exim.core.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.Value;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.onehippo.forge.content.exim.core.ContentMigrationException;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.ContentMigrationTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.util.FileFilterDepthSelector;
import org.onehippo.forge.content.exim.core.util.NamePatternFileFilter;
import org.onehippo.forge.content.pojo.common.ContentValueConverter;
import org.onehippo.forge.content.pojo.common.jcr.DefaultJcrContentValueConverter;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract {@link ContentMigrationTask} implementation class to provide common properties and utility operations.
 */
abstract public class AbstractContentMigrationTask implements ContentMigrationTask {

    private Logger logger = LoggerFactory.getLogger(AbstractContentMigrationTask.class);

    private static ThreadLocal<ContentMigrationRecord> tlCurrentContentMigrationRecord = new ThreadLocal<>();

    private long startedTimeMillis;
    private long stoppedTimeMillis;
    private List<ContentMigrationRecord> contentMigrationRecords = new LinkedList<>();

    private final DocumentManager documentManager;
    private ObjectMapper objectMapper;
    private ContentValueConverter<Value> contentValueConverter;
    private FileObject binaryValueFileFolder;
    private long dataUrlSizeThreashold = 512 * 1024; // 512 KB

    /**
     * Constructs with {@code documentManager}.
     * @param documentManager {@link DocumentManager} instance
     */
    public AbstractContentMigrationTask(final DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (isStarted()) {
            throw new IllegalStateException("Task was already started.");
        }

        startedTimeMillis = System.currentTimeMillis();
        stoppedTimeMillis = 0L;
        contentMigrationRecords.clear();
        tlCurrentContentMigrationRecord.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!isStarted()) {
            throw new IllegalStateException("Task was not started.");
        }

        stoppedTimeMillis = System.currentTimeMillis();
        tlCurrentContentMigrationRecord.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartedTimeMillis() {
        return startedTimeMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStoppedTimeMillis() {
        return stoppedTimeMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentMigrationRecord beginRecord(String contentId, String contentPath) {
        ContentMigrationRecord record = new ContentMigrationRecord();
        record.setContentId(contentId);
        record.setContentPath(contentPath);
        contentMigrationRecords.add(record);
        tlCurrentContentMigrationRecord.set(record);
        return record;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentMigrationRecord endRecord() {
        ContentMigrationRecord record = getCurrentContentMigrationRecord();
        tlCurrentContentMigrationRecord.remove();
        return record;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ContentMigrationRecord> getContentMigrationRecords() {
        return Collections.unmodifiableCollection(contentMigrationRecords);
    }

    /**
     * Returns the current {@link ContentMigrationRecord} instance in the current context thread.
     * @return the current {@link ContentMigrationRecord} instance in the current context thread
     */
    public static ContentMigrationRecord getCurrentContentMigrationRecord() {
        return tlCurrentContentMigrationRecord.get();
    }

    @Override
    public String getSummary() {
        StringWriter sw = new StringWriter(1024);
        PrintWriter out = new PrintWriter(sw);
        CSVPrinter csvPrinter = null;

        try {
            sw = new StringWriter(1024);
            out = new PrintWriter(sw);

            int totalCount = 0;
            int processedCount = 0;
            int successCount = 0;

            for (ContentMigrationRecord record : getContentMigrationRecords()) {
                ++totalCount;

                if (record.isProcessed()) {
                    ++processedCount;

                    if (record.isSucceeded()) {
                        ++successCount;
                    }
                }
            }

            out.println(
                    "===============================================================================================================");
            out.println("Execution Summary:");
            out.println(
                    "---------------------------------------------------------------------------------------------------------------");
            out.printf("Total: %d, Processed: %d, Suceeded: %d, Failed: %d, Duration: %dms", totalCount, processedCount,
                    successCount, processedCount - successCount, getStoppedTimeMillis() - getStartedTimeMillis());
            out.println();
            out.println(
                    "---------------------------------------------------------------------------------------------------------------");
            out.println("Details (in CSV format):");
            out.println(
                    "---------------------------------------------------------------------------------------------------------------");

            try {
                csvPrinter = CSVFormat.DEFAULT
                        .withHeader("SEQ", "PROCESSED", "SUCCEEDED", "ID", "PATH", "TYPE", "ATTRIBUTES", "ERROR")
                        .print(out);

                int seq = 0;

                for (ContentMigrationRecord record : getContentMigrationRecords()) {
                    csvPrinter.printRecord(++seq, record.isProcessed(), record.isSucceeded(),
                            StringUtils.defaultString(record.getContentId()),
                            StringUtils.defaultString(record.getContentPath()),
                            StringUtils.defaultString(record.getContentType()),
                            ObjectUtils.toString(record.getAttributeMap()),
                            StringUtils.defaultString(record.getErrorMessage()));
                }
            } catch (IOException e) {
                e.printStackTrace(out);
            }

            out.println(
                    "===============================================================================================================");
            out.flush();

            return sw.toString();
        } finally {
            IOUtils.closeQuietly(csvPrinter);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(sw);
        }
    }

    /**
     * {@inheritDoc}
     * <P>
     * The default implementation summarizes the result with details on each execution record in CSV format.
     * </P>
     */
    @Override
    public void logSummary() {
        getLogger().info("\n\n{}\n", getSummary());
    }

    /**
     * Returns {@link DocumentManager} instance.
     * @return {@link DocumentManager} instance
     */
    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    /**
     * Returns {@link ObjectMapper} instance. If not set, returns a default {@link ObjectMapper} instance.
     * @return {@link ObjectMapper} instance
     */
    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    /**
     * Sets {@link ObjectMapper} instance.
     * @param objectMapper {@link ObjectMapper} instance
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns {@link ContentValueConverter} instance. If not set, returns a default {@link ContentValueConverter} instance.
     * @return {@link ContentValueConverter} instance
     */
    public ContentValueConverter<Value> getContentValueConverter() {
        if (contentValueConverter == null) {
            contentValueConverter = new DefaultJcrContentValueConverter(getDocumentManager().getSession());
            ((DefaultJcrContentValueConverter) contentValueConverter)
                    .setBinaryValueFileFolder(getBinaryValueFileFolder());
            ((DefaultJcrContentValueConverter) contentValueConverter)
                    .setDataUrlSizeThreashold(getDataUrlSizeThreashold());
        }

        return contentValueConverter;
    }

    /**
     * Sets {@link ContentValueConverter}.
     * @param contentValueConverter {@link ContentValueConverter} instance
     */
    public void setContentValueConverter(ContentValueConverter<Value> contentValueConverter) {
        this.contentValueConverter = contentValueConverter;
    }

    /**
     * Returns base folder object of type {@link FileObject}, which is used as a base folder
     * when storing a binary value into an external file.
     * @return base folder object of type {@link FileObject}
     */
    public FileObject getBinaryValueFileFolder() {
        return binaryValueFileFolder;
    }

    /**
     * Sets the base folder object of type {@link FileObject}, which is used as a base folder
     * when storing a binary value into an external file.
     * @param binaryValueFileFolder base folder object of type {@link FileObject}
     */
    public void setBinaryValueFileFolder(FileObject binaryValueFileFolder) {
        this.binaryValueFileFolder = binaryValueFileFolder;

        if (contentValueConverter != null && contentValueConverter instanceof DefaultJcrContentValueConverter) {
            ((DefaultJcrContentValueConverter) contentValueConverter).setBinaryValueFileFolder(binaryValueFileFolder);
        }
    }

    /**
     * Returns the threshold binary value data size used to determine if the specific binary value data should
     * be stored in either an embedded data: url inside a {@link ContentNode} object or an external file
     * when the data size is bigger than the threshold.
     * @return the threshold binary value data size used to determine if the specific binary value data should
     *         be stored in either an embedded data: url inside a {@link ContentNode} object or an external file
     *         when the data size is bigger than the threshold.
     */
    public long getDataUrlSizeThreashold() {
        return dataUrlSizeThreashold;
    }

    /**
     * Sets the threshold binary value data size used to determine if the specific binary value data should
     * be stored in either an embedded data: url inside a {@link ContentNode} object or an external file
     * when the data size is bigger than the threshold.
     * @param dataUrlSizeThreashold the threshold binary value data size used to determine if the specific binary value data should
     *        be stored in either an embedded data: url inside a {@link ContentNode} object or an external file
     *        when the data size is bigger than the threshold.
     */
    public void setDataUrlSizeThreashold(long dataUrlSizeThreashold) {
        this.dataUrlSizeThreashold = dataUrlSizeThreashold;

        if (contentValueConverter != null && contentValueConverter instanceof DefaultJcrContentValueConverter) {
            ((DefaultJcrContentValueConverter) contentValueConverter)
                    .setDataUrlSizeThreashold(getDataUrlSizeThreashold());
        }
    }

    /**
     * Finds files (type of {@link FileObject}) under {@code baseFolder} based on the file name pattern specified by the regular expression, {@code nameRegex}.
     * It only matches when the depth of a descendant file in the range, [{@code minDepth}, {@code maxDepth}].
     * @param baseFolder base folder to search from
     * @param nameRegex file name pattern regular expression
     * @param minDepth minimum depth of a descendant file
     * @param maxDepth maximum depth of a descendant file
     * @return array of files (type of {@link FileObject}) found
     * @throws FileSystemException if any file system exception occurs
     */
    public FileObject[] findFilesByNamePattern(FileObject baseFolder, String nameRegex, int minDepth, int maxDepth)
            throws FileSystemException {
        final FileFilter fileFilter = new NamePatternFileFilter(Pattern.compile(nameRegex));
        final FileSelector selector = new FileFilterDepthSelector(fileFilter, minDepth, maxDepth);
        return baseFolder.findFiles(selector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNode readContentNodeFromJsonFile(final FileObject sourceFile) throws ContentMigrationException {
        ContentNode contentNode = null;

        InputStream is = null;
        BufferedInputStream bis = null;

        try {
            is = sourceFile.getContent().getInputStream();
            bis = new BufferedInputStream(is);
            contentNode = getObjectMapper().readValue(bis, ContentNode.class);
        } catch (IOException e) {
            throw new ContentMigrationException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        return contentNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentNode readContentNodeFromXmlFile(final FileObject sourceFile) throws ContentMigrationException {
        ContentNode contentNode = null;

        InputStream is = null;
        BufferedInputStream bis = null;

        try {
            is = sourceFile.getContent().getInputStream();
            bis = new BufferedInputStream(is);
            JAXBContext jaxbContext = JAXBContext.newInstance(ContentNode.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            contentNode = (ContentNode) jaxbUnmarshaller.unmarshal(bis);
        } catch (IOException | JAXBException e) {
            throw new ContentMigrationException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        return contentNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContentNodeToJsonFile(final ContentNode contentNode, final FileObject targetFile)
            throws ContentMigrationException {
        OutputStream os = null;
        BufferedOutputStream bos = null;

        try {
            os = targetFile.getContent().getOutputStream();
            bos = new BufferedOutputStream(os);
            getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(bos, contentNode);
        } catch (IOException e) {
            throw new ContentMigrationException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContentNodeToXmlFile(final ContentNode contentNode, final FileObject targetFile)
            throws ContentMigrationException {
        OutputStream os = null;
        BufferedOutputStream bos = null;

        try {
            os = targetFile.getContent().getOutputStream();
            bos = new BufferedOutputStream(os);
            JAXBContext jaxbContext = JAXBContext.newInstance(ContentNode.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(contentNode, bos);
        } catch (IOException | JAXBException e) {
            throw new ContentMigrationException(e.toString(), e);
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * Returns true if this task was already started.
     * @return true if this task was already started
     */
    private boolean isStarted() {
        return startedTimeMillis != 0;
    }

}
