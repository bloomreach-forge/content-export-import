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
package org.onehippo.forge.content.exim.repository.jaxrs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AbstractContentEximService.
 */
public abstract class AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(AbstractContentEximService.class);

    /**
     * System session credentials.
     */
    protected static final Credentials SYSTEM_CREDENTIALS = new SimpleCredentials("system", new char[] {});

    /**
     * Prefix of the temporary folder or files. e.g, temporary folder in zip content creation.
     */
    protected static final String TEMP_PREFIX = "_exim_";

    /**
     * Zip Entry name of the summary log for binaries.
     */
    protected static final String EXIM_SUMMARY_BINARIES_LOG_REL_PATH = "EXIM-INF/summary-binaries.log";

    /**
     * Zip Entry name of the summary log for documents.
     */
    protected static final String EXIM_SUMMARY_DOCUMENTS_LOG_REL_PATH = "EXIM-INF/summary-documents.log";

    /**
     * Zip Entry name prefix for the binary attachments.
     */
    protected static final String BINARY_ATTACHMENT_REL_PATH = "EXIM-INF/data/attachments";

    /**
     * Stop signal file's relative path under the zip creating base folder.
     * If this file is found in the process, the export or import process will stop right away.
     */
    protected static final String STOP_REQUEST_FILE_REL_PATH = "EXIM-INF/_stop_";

    /**
     * Jackson ObjectMapper instance.
     */
    private ObjectMapper objectMapper;

    /**
     * JCR session given by the DaemonModule and used to create a new system session from it.
     */
    private Session daemonSession;

    /**
     * Default constructor.
     */
    public AbstractContentEximService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    /**
     * Return the default Jackson ObjectMapper instance.
     * @return ObjectMapper instance
     */
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Return the JCR session given by the DaemonModule.
     * It is not supposed to use this JCR session directly to retrieve or manipulate content.
     * You should use {@link #createSession()} for that purpose instead.
     * @return the JCR session given by the DaemonModule
     */
    protected Session getDaemonSession() {
        return daemonSession;
    }

    /**
     * Set the JCR session, supposed to be set by the DaemonModule.
     * @param daemonSession the JCR session given by the DaemonModule
     */
    protected void setDaemonSession(Session daemonSession) {
        this.daemonSession = daemonSession;
    }

    /**
     * Create a new JCR system session by impersonating the JCR session returned from {@link #getDaemonSession()}.
     * @return a new JCR system session by impersonating the JCR session returned from {@link #getDaemonSession()}
     * @throws LoginException if impersonation with system credentials fails
     * @throws RepositoryException if repository exception occurs
     */
    protected Session createSession() throws LoginException, RepositoryException {
        return getDaemonSession().impersonate(SYSTEM_CREDENTIALS);
    }

    /**
     * Return true if a stop signal file is found under the base folder.
     * @param baseFolder the base folder where zip content files are created temporarily.
     * @return true if a stop signal file is found under the base folder
     */
    protected boolean isStopRequested(FileObject baseFolder) {
        try {
            FileObject stopSignalFile = baseFolder.resolveFile(STOP_REQUEST_FILE_REL_PATH);
            return stopSignalFile.exists();
        } catch (Exception e) {
            log.error("Failed to check stop request file.", e);
        }

        return false;
    }

    /**
     * Return a JSON string by stringifying the {@code object} with the Jackson ObjectMapper.
     * @param object object to stringify
     * @return a JSON string by stringifying the {@code object} with the Jackson ObjectMapper
     * @throws JsonProcessingException if JSON stringifying fails
     */
    protected String toJsonString(Object object) throws JsonProcessingException {
        return getObjectMapper().writeValueAsString(object);
    }

    /**
     * Read the CXF attachment JAX-RS argument and convert it to a string.
     * @param attachment CXF attachment JAX-RS argument
     * @param charsetName charset name used in encoding
     * @return string converted from the CXF attachment JAX-RS argument
     * @throws IOException if IO exception occurs
     */
    protected String attachmentToString(Attachment attachment, String charsetName) throws IOException {
        InputStream input = null;

        try {
            input = attachment.getObject(InputStream.class);
            return IOUtils.toString(input, charsetName);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * Transfer attachment content into the given {@code file}.
     * @param attachment attachment
     * @param file destination file
     * @throws IOException if IO exception occurs
     */
    protected void transferAttachmentToFile(Attachment attachment, File file) throws IOException {
        InputStream input = null;
        OutputStream output = null;

        try {
            input = attachment.getObject(InputStream.class);
            output = new FileOutputStream(file);
            IOUtils.copyLarge(input, output);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * Convert the {@link ContentMigrationRecord} instance to a {@link ResultItem} instance.
     * @param record a {@link ContentMigrationRecord} instance
     * @return a converted {@link ResultItem} instance
     */
    protected ResultItem recordToResultItem(ContentMigrationRecord record) {
        ResultItem item = new ResultItem(record.getContentPath(), record.getContentType());
        item.setSucceeded(record.isSucceeded());
        item.setErrorMessage(record.getErrorMessage());
        return item;
    }

    /**
     * Executes JCR query using the query {@code statement} in the query {@code language} and collect all the result
     * node paths in a set to return.
     * @param session JCR session
     * @param statement JCR query statement
     * @param language JCR query language
     * @return a set containing all the nodes from the query result
     * @throws RepositoryException if repository exception occurs
     */
    protected Set<String> getQueriedNodePaths(Session session, String statement, String language)
            throws RepositoryException {
        Set<String> nodePaths = new LinkedHashSet<>();
        Query query = session.getWorkspace().getQueryManager().createQuery(statement, language);
        QueryResult result = query.execute();

        for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
            Node node = nodeIt.nextNode();

            if (node != null) {
                nodePaths.add(node.getPath());
            }
        }

        return nodePaths;
    }

    /**
     * Override {@code params} by the give request parameter values.
     * @param params {@link ExecutionParams} instance
     * @param batchSizeParam batch size request parameter value
     * @param throttleParam throttle request parameter value
     * @param publishOnImportParam publishOnImport request parameter value
     * @param dataUrlSizeThresholdParam dataUrlSizeThreshold request parameter value
     * @param docbasePropNamesParam docbasePropNames request parameter value
     * @param documentTagsParam documentTags request parameter value
     * @param binaryTagsParam binaryTags request parameter value
     */
    protected void overrideExecutionParamsByParameters(ExecutionParams params, String batchSizeParam,
            String throttleParam, String publishOnImportParam, String dataUrlSizeThresholdParam,
            String docbasePropNamesParam, String documentTagsParam, String binaryTagsParam) {
        if (StringUtils.isNotBlank(batchSizeParam)) {
            params.setBatchSize(NumberUtils.toInt(batchSizeParam, params.getBatchSize()));
        }

        if (StringUtils.isNotBlank(throttleParam)) {
            params.setThrottle(NumberUtils.toLong(throttleParam, params.getThrottle()));
        }

        if (StringUtils.isNotBlank(publishOnImportParam)) {
            params.setPublishOnImport(BooleanUtils.toBoolean(publishOnImportParam));
        }

        if (StringUtils.isNotBlank(dataUrlSizeThresholdParam)) {
            params.setDataUrlSizeThreshold(
                    NumberUtils.toLong(dataUrlSizeThresholdParam, params.getDataUrlSizeThreshold()));
        }

        if (StringUtils.isNotBlank(docbasePropNamesParam)) {
            params.setDocbasePropNames(new LinkedHashSet<>(Arrays.asList(StringUtils.split(docbasePropNamesParam, ","))));
        }

        if (StringUtils.isNotBlank(documentTagsParam)) {
            params.setDocumentTags(new LinkedHashSet<>(Arrays.asList(StringUtils.split(documentTagsParam, ";"))));
        }

        if (StringUtils.isNotBlank(binaryTagsParam)) {
            params.setBinaryTags(new LinkedHashSet<>(Arrays.asList(StringUtils.split(binaryTagsParam, ";"))));
        }
    }

    /**
     * Find the attachment in {@code attachments} list by the {@code contentId}.
     * @param attachments attachment list
     * @param contentId content Id
     * @return the attachment in {@code attachments} list found by the {@code contentId}
     */
    protected Attachment getAttachmentByContentId(List<Attachment> attachments, String contentId) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        for (Attachment attachment : attachments) {
            if (StringUtils.equals(contentId, attachment.getContentId())) {
                return attachment;
            }
            ContentDisposition contentDisposition = attachment.getContentDisposition();
            if (contentDisposition != null && StringUtils.equals(contentId, contentDisposition.getParameter("name"))) {
                return attachment;
            }
        }

        return null;
    }

    /**
     * Apply tag field on the content node with give {@code tagInfos} list, each item of which should look like
     * "myhippoproject:tags=a,b,c".
     * @param contentNode content node
     * @param tagInfos tag info line like "myhippoproject:tags=a,b,c"
     * @return true if any tag field is added
     */
    protected boolean applyTagContentProperties(ContentNode contentNode, Set<String> tagInfos) {
        if (CollectionUtils.isEmpty(tagInfos)) {
            return false;
        }

        boolean updated = false;

        for (String tagInfo : tagInfos) {
            String name = StringUtils.substringBefore(tagInfo, "=");
            String values = StringUtils.substringAfter(tagInfo, "=");

            if (StringUtils.isBlank(name) || StringUtils.isBlank(values)) {
                log.warn("Invalid content tag info: {}", tagInfo);
                continue;
            }

            contentNode.setProperty(name, StringUtils.split(values, ","));
            updated = true;
        }

        return updated;
    }

}
