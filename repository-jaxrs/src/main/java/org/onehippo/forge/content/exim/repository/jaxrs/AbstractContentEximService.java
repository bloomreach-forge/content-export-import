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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
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
     * Prefix of the temporary folder for zip creation.
     */
    protected static final String ZIP_TEMP_BASE_PREFIX = "_exim_";

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
     * Default gallery folder's primary node type name.
     */
    protected static final String DEFAULT_GALLERY_FOLDER_PRIMARY_TYPE = "hippogallery:stdImageGallery";

    /**
     * Default foldertypes property values of a gallery folder.
     */
    protected static final String[] DEFAULT_GALLERY_FOLDER_FOLDER_TYPES = { "new-image-folder" };

    /**
     * Default gallerytypes property values of a gallery folder.
     */
    protected static final String[] DEFAULT_GALLERY_GALLERY_TYPES = { "hippogallery:imageset" };

    /**
     * Default asset folder's primary node type name.
     */
    protected static final String DEFAULT_ASSET_FOLDER_PRIMARY_TYPE = "hippogallery:stdAssetGallery";

    /**
     * Default foldertypes property values of an asset folder.
     */
    protected static final String[] DEFAULT_ASSET_FOLDER_FOLDER_TYPES = { "new-file-folder" };

    /**
     * Default gallerytypes property values of an asset folder.
     */
    protected static final String[] DEFAULT_ASSET_GALLERY_TYPES = { "hippogallery:exampleAssetSet" };

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
     * @return
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
            input = attachment.getDataHandler().getInputStream();
            return IOUtils.toString(input, charsetName);
        } finally {
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
    protected Set<String> getQueriedNodePaths(Session session, String statement, String language) throws RepositoryException {
        Set<String> nodePaths = new LinkedHashSet<>();
        Query query = session.getWorkspace().getQueryManager().createQuery(statement, language);
        QueryResult result = query.execute();

        for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
            Node node = nodeIt.nextNode();

            if (node != null) {
                nodePaths.add(node.getPath());
            }
        }

        return nodePaths;
    }
}
