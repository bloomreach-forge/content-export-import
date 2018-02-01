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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

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

public abstract class AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(AbstractContentEximService.class);

    protected static final Credentials SYSTEM_CREDENTIALS = new SimpleCredentials("system", new char[] {});

    protected static final String ZIP_TEMP_BASE_PREFIX = "_exim_";

    protected static final String EXIM_SUMMARY_BINARIES_LOG_REL_PATH = "EXIM-INF/summary-binaries.log";

    protected static final String EXIM_SUMMARY_DOCUMENTS_LOG_REL_PATH = "EXIM-INF/summary-documents.log";

    protected static final String BINARY_ATTACHMENT_REL_PATH = "EXIM-INF/data/attachments";

    protected static final String STOP_REQUEST_FILE_REL_PATH = "EXIM-INF/_stop_";

    protected static final String DEFAULT_GALLERY_FOLDER_PRIMARY_TYPE = "hippogallery:stdImageGallery";
    protected static final String[] DEFAULT_GALLERY_FOLDER_FOLDER_TYPES = { "new-image-folder" };
    protected static final String[] DEFAULT_GALLERY_GALLERY_TYPES = { "hippogallery:imageset" };

    protected static final String DEFAULT_ASSET_FOLDER_PRIMARY_TYPE = "hippogallery:stdAssetGallery";
    protected static final String[] DEFAULT_ASSET_FOLDER_FOLDER_TYPES = { "new-file-folder" };
    protected static final String[] DEFAULT_ASSET_GALLERY_TYPES = { "hippogallery:exampleAssetSet" };

    private ObjectMapper objectMapper;

    private Session daemonSession;

    public AbstractContentEximService() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected Session getDaemonSession() {
        return daemonSession;
    }

    protected void setDaemonSession(Session daemonSession) {
        this.daemonSession = daemonSession;
    }

    protected Session createSession() throws LoginException, RepositoryException {
        return getDaemonSession().impersonate(SYSTEM_CREDENTIALS);
    }

    protected boolean isStopRequested(FileObject baseFolder) {
        try {
            FileObject stopSignalFile = baseFolder.resolveFile(STOP_REQUEST_FILE_REL_PATH);
            return stopSignalFile.exists();
        } catch (Exception e) {
            log.error("Failed to check stop request file.", e);
        }

        return false;
    }

    protected String toJsonString(Object object) throws JsonProcessingException {
        return getObjectMapper().writeValueAsString(object);
    }

    protected String attachmentToString(Attachment attachment, String charsetName) throws IOException {
        InputStream input = null;

        try {
            input = attachment.getDataHandler().getInputStream();
            return IOUtils.toString(input, charsetName);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    protected ResultItem recordToResultItem(ContentMigrationRecord record) {
        ResultItem item = new ResultItem(record.getContentPath(), record.getContentType());
        item.setSucceeded(record.isSucceeded());
        item.setErrorMessage(record.getErrorMessage());
        return item;
    }
}
