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
package org.onehippo.forge.content.exim.repository.jaxrs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.vfs2.FileObject;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.onehippo.cms7.utilities.logging.PrintStreamLogger;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.util.AntPathMatcher;
import org.onehippo.forge.content.exim.core.util.TeeLoggerWrapper;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ServletRequestUtils;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
     * The whole execution log file entry name.
     */
    protected static final String EXIM_EXECUTION_LOG_REL_PATH = "EXIM-INF/execution.log";

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

    private ProcessMonitor processMonitor;

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
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    protected void setProcessMonitor(ProcessMonitor processMonitor) {
        this.processMonitor = processMonitor;
    }

    protected ProcessMonitor getProcessMonitor() {
        return processMonitor;
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
            params.setPublishOnImport(publishOnImportParam);
        }

        if (StringUtils.isNotBlank(dataUrlSizeThresholdParam)) {
            params.setDataUrlSizeThreshold(
                    NumberUtils.toLong(dataUrlSizeThresholdParam, params.getDataUrlSizeThreshold()));
        }

        if (StringUtils.isNotBlank(docbasePropNamesParam)) {
            params.setDocbasePropNames(
                    new LinkedHashSet<>(Arrays.asList(StringUtils.split(docbasePropNamesParam, ","))));
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

    /**
     * Find user principal's name from {@code securityContext} or {@code request}.
     * @param securityContext security context
     * @param request servlet request
     * @return user principal's name from {@code securityContext} or {@code request}
     */
    protected String getUserPrincipalName(SecurityContext securityContext, HttpServletRequest request) {
        if (securityContext != null) {
            Principal userPrincipal = securityContext.getUserPrincipal();
            if (userPrincipal != null) {
                return userPrincipal.getName();
            }
        }

        if (request != null) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                return userPrincipal.getName();
            }

            final String authHeader = request.getHeader("Authorization");

            if (StringUtils.isNotBlank(authHeader)) {
                if (StringUtils.startsWithIgnoreCase(authHeader, "Basic ")) {
                    final String encoded = authHeader.substring(6).trim();
                    final String decoded = new String(Base64.getDecoder().decode(encoded));
                    return StringUtils.substringBefore(decoded, ":");
                }
            }
        }

        return null;
    }

    /**
     * Fill basic info from {@code securityContext} and {@code request} in {@code process}.
     * @param process process
     * @param securityContext security context
     * @param request servlet request
     */
    protected void fillProcessStatusByRequestInfo(ProcessStatus process, SecurityContext securityContext,
            HttpServletRequest request) {
        process.setUsername(getUserPrincipalName(securityContext, request));
        process.setClientInfo(ServletRequestUtils.getFarthestRemoteAddr(request));

        StringBuilder sbCommand = new StringBuilder(256).append(request.getMethod()).append(' ')
                .append(request.getRequestURI());
        String queryString = request.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            sbCommand.append('?').append(queryString);
        }
        process.setCommandInfo(sbCommand.toString());
    }

    /**
     * Create a tee-ing logger.
     * @param mainLogger main logger
     * @param secondOutput output for the second logger
     * @return a tee-ing logger
     */
    protected Logger createTeeLogger(final Logger mainLogger, final PrintStream secondOutput) {
        final Logger second = new TimestampPrintStreamLogger("exim", PrintStreamLogger.INFO_LEVEL, secondOutput);
        return new TeeLoggerWrapper(mainLogger, second);
    }

    /**
     * Return true if the given {@code path} is included in the {@code param}'s binary path includes parameter.
     * @param pathMatcher AntPathMatcher instance
     * @param params Execution params
     * @param path binary path
     * @return true if the given {@code path} is included in the {@code param}'s binary path includes parameter
     */
    protected boolean isBinaryPathIncluded(final AntPathMatcher pathMatcher, final ExecutionParams params,
            final String path) {
        QueriesAndPaths queriesAndPaths = params.getBinaries();

        if (queriesAndPaths == null) {
            return true;
        }

        return isPathIncluded(pathMatcher, queriesAndPaths.getExcludes(), queriesAndPaths.getIncludes(), path);
    }

    /**
     * Return true if the given {@code path} is included in the {@code param}'s document path includes parameter.
     * @param pathMatcher AntPathMatcher instance
     * @param params Execution params
     * @param path document path
     * @return true if the given {@code path} is included in the {@code param}'s document path includes parameter
     */
    protected boolean isDocumentPathIncluded(final AntPathMatcher pathMatcher, final ExecutionParams params,
            final String path) {
        QueriesAndPaths queriesAndPaths = params.getDocuments();

        if (queriesAndPaths == null) {
            return true;
        }

        return isPathIncluded(pathMatcher, queriesAndPaths.getExcludes(), queriesAndPaths.getIncludes(), path);
    }

    private boolean isPathIncluded(final AntPathMatcher pathMatcher, final Collection<String> excludes,
            final Collection<String> includes, final String path) {
        if (CollectionUtils.isNotEmpty(excludes)) {
            for (String exclude : excludes) {
                if (pathMatcher.match(exclude, path)) {
                    return false;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(includes)) {
            for (String include : includes) {
                if (pathMatcher.match(include, path)) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private static class TimestampPrintStreamLogger extends PrintStreamLogger {

        private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss,SSS");

        public TimestampPrintStreamLogger(final String name, final int level, final PrintStream... out)
                throws IllegalArgumentException {
            super(name, level, out);
        }

        @Override
        protected String getMessageString(final String level, final String message) {
            final String ts = dateFormat.format(System.currentTimeMillis());
            return level + " " + ts + " " + message;
        }
    }
}
