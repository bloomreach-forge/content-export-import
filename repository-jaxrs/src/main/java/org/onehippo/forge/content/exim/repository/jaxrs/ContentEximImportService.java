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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.query.Query;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.impl.DefaultBinaryImportTask;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentManagerImpl;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentVariantImportTask;
import org.onehippo.forge.content.exim.core.util.AntPathMatcher;
import org.onehippo.forge.content.exim.core.util.ContentNodeUtils;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoBinaryNodeUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Content-EXIM Import JAX-RS Service.
 */
@Path("/import")
public class ContentEximImportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximImportService.class);

    public ContentEximImportService() {
        super();
    }

    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("multipart/mixed")
    @POST
    public Response importContentFromZip(@Context SecurityContext securityContext, @Context HttpServletRequest request,
            @Multipart(value = "batchSize", required = false) String batchSizeParam,
            @Multipart(value = "throttle", required = false) String throttleParam,
            @Multipart(value = "publishOnImport", required = false) String publishOnImportParam,
            @Multipart(value = "dataUrlSizeThreshold", required = false) String dataUrlSizeThresholdParam,
            @Multipart(value = "docbasePropNames", required = false) String docbasePropNamesParam,
            @Multipart(value = "documentTags", required = false) String documentTagsParam,
            @Multipart(value = "binaryTags", required = false) String binaryTagsParam,
            @Multipart(value = "paramsJson", required = false) String paramsJsonParam,
            @Multipart(value = "params", required = false) Attachment paramsAttachment,
            @Multipart(value = "package", required = true) Attachment packageAttachment)
            throws JsonProcessingException {

        List<Attachment> attachments = new ArrayList<>();

        Logger procLogger = log;

        Result result = new Result();

        File tempLogFile = null;
        PrintStream tempLogOut = null;
        // The physical uploaded zip java.io.file.
        File tempZipFile = null;
        // The logical zip file folder in commons-VFS FileObject. This is the reading source.
        FileObject baseFolder = null;
        Session session = null;
        ExecutionParams params = new ExecutionParams();
        ProcessStatus processStatus = null;

        try {
            tempLogFile = File.createTempFile(TEMP_PREFIX, ".log");
            tempLogOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(tempLogFile)));
            procLogger = createTeeLogger(log, tempLogOut);

            if (getProcessMonitor() != null) {
                processStatus = getProcessMonitor().startProcess();
                fillProcessStatusByRequestInfo(processStatus, securityContext, request);
                processStatus.setLogFile(tempLogFile);
            }

            tempZipFile = File.createTempFile(TEMP_PREFIX, ".zip");
            procLogger.info("ContentEximService#importContentFromZip begins with {}", tempZipFile.getPath());

            if (packageAttachment == null) {
                result.addError("No zip attachment.");
                return Response.serverError().entity(toJsonString(result)).build();
            }

            if (paramsAttachment != null) {
                final String json = attachmentToString(paramsAttachment, "UTF-8");
                if (StringUtils.isNotBlank(json)) {
                    params = getObjectMapper().readValue(json, ExecutionParams.class);
                }
            } else {
                if (StringUtils.isNotBlank(paramsJsonParam)) {
                    params = getObjectMapper().readValue(paramsJsonParam, ExecutionParams.class);
                }
            }
            overrideExecutionParamsByParameters(params, batchSizeParam, throttleParam, publishOnImportParam,
                    dataUrlSizeThresholdParam, docbasePropNamesParam, documentTagsParam, binaryTagsParam);

            if (processStatus != null) {
                processStatus.setExecutionParams(params);
            }

            transferAttachmentToFile(packageAttachment, tempZipFile);

            baseFolder = VFS.getManager().resolveFile("zip:" + tempZipFile.toURI());

            session = createSession();

            DocumentManager documentManager = new WorkflowDocumentManagerImpl(session);

            final DefaultBinaryImportTask binaryImportTask = new DefaultBinaryImportTask(documentManager);
            binaryImportTask.setLogger(procLogger);

            final WorkflowDocumentVariantImportTask documentImportTask = new WorkflowDocumentVariantImportTask(
                    documentManager);
            documentImportTask.setLogger(procLogger);

            FileObject[] jsonFiles = binaryImportTask.findFilesByNamePattern(baseFolder, "^.+\\.json$", 1, 20);

            int batchCount = 0;

            try {
                binaryImportTask.start();
                batchCount = importBinaries(procLogger, processStatus, jsonFiles, params, baseFolder, binaryImportTask,
                        result, batchCount);
            } finally {
                binaryImportTask.stop();
            }

            try {
                documentImportTask.start();
                batchCount = importDocuments(procLogger, processStatus, jsonFiles, params, baseFolder,
                        documentImportTask, result, batchCount);
            } finally {
                documentImportTask.stop();
            }

            batchCount = cleanMirrorDocbaseValues(procLogger, processStatus, session, params, result, batchCount);
            batchCount = cleanAllDocbaseFieldValues(procLogger, processStatus, session, params, result, batchCount);

            if (processStatus != null) {
                processStatus.setProgress(1.0);
            }

            procLogger.info("ContentEximService#importContentFromZip ends.");

            attachments.add(
                    new Attachment("logs", MediaType.TEXT_PLAIN, FileUtils.readFileToString(tempLogFile, "UTF-8")));
            attachments.add(new Attachment("summary", MediaType.APPLICATION_JSON, toJsonString(result)));

        } catch (Exception e) {
            procLogger.error("Failed to import content.", e);
            result.addError(e.toString());
            return Response.serverError().entity(toJsonString(result)).build();
        } finally {
            procLogger.info("ContentEximService#importContentFromZip finally ends.");

            if (getProcessMonitor() != null) {
                try {
                    getProcessMonitor().stopProcess(processStatus);
                } catch (Exception e) {
                    procLogger.error("Failed to stop process.", e);
                }
            }

            if (session != null) {
                try {
                    session.logout();
                } catch (Exception e) {
                    procLogger.error("Failed to logout JCR session.", e);
                }
            }

            // NOTE: Close the open connection to the logical VFS FileObject folder wrapping the temp zip file.
            //       Otherwise, the open file descriptor by the zip VFS FileObject doesn't seem to be released,
            //       and so OS cannot remove the temp zip file.
            if (baseFolder != null) {
                try {
                    baseFolder.close();  // Contributed by Freenet (Dev: Mark Kaloukh)
                } catch (Exception e) {
                    procLogger.error("Failed to remove VFS zip file folder lock.", e);
                }
            }

            if (tempZipFile != null) {
                try {
                    tempZipFile.delete();
                } catch (Exception e) {
                    procLogger.error("Failed to delete temporary zip file.", e);
                }
            }

            if (tempLogOut != null) {
                IOUtils.closeQuietly(tempLogOut);
            }

            if (tempLogFile != null) {
                try {
                    tempLogFile.delete();
                } catch (Exception e) {
                    log.error("Failed to delete temporary log file.", e);
                }
            }
        }

        return Response.ok(new MultipartBody(attachments, true)).build();
    }

    private int importBinaries(Logger procLogger, ProcessStatus processStatus, FileObject[] jsonFiles,
            ExecutionParams params, FileObject baseFolder, DefaultBinaryImportTask importTask, Result result,
            int batchCount) throws Exception {
        final String baseFolderUrlPrefix = baseFolder.getURL().toString();
        final AntPathMatcher pathMatcher = new AntPathMatcher();

        for (FileObject file : jsonFiles) {
            if (isStopRequested(baseFolder)) {
                procLogger.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(),
                        STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentNode contentNode = importTask.readContentNodeFromJsonFile(file);

            String primaryTypeName = contentNode.getPrimaryType();
            String path = contentNode.getProperty("jcr:path").getValue();

            if (!isBinaryPathIncluded(pathMatcher, params, path)) {
                continue;
            }

            if (!HippoNodeUtils.isBinaryPath(path)) {
                continue;
            }

            ContentMigrationRecord record = null;

            try {
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH,
                        baseFolderUrlPrefix);

                record = importTask.beginRecord("", path);
                record.setAttribute("file", file.getName().getPath());
                record.setProcessed(true);

                String[] folderPathAndName = ContentPathUtils.splitToFolderPathAndName(path);
                String folderPath = folderPathAndName[0];
                String name = folderPathAndName[1];

                String folderPrimaryType;
                String[] folderTypes;
                String[] galleryTypes;

                if (HippoNodeUtils.isGalleryPath(path)) {
                    folderPrimaryType = params.getGalleryFolderPrimaryType();
                    folderTypes = params.getGalleryFolderFolderTypes();
                    galleryTypes = params.getGalleryFolderGalleryTypes();
                } else {
                    folderPrimaryType = params.getAssetFolderPrimaryType();
                    folderTypes = params.getAssetFolderFolderTypes();
                    galleryTypes = params.getAssetFolderGalleryTypes();
                }

                folderPath = importTask.createOrUpdateBinaryFolder(folderPath, folderPrimaryType, folderTypes,
                        galleryTypes);

                applyTagContentProperties(contentNode, params.getBinaryTags());

                String updatedPath = importTask.createOrUpdateBinaryFromContentNode(contentNode, primaryTypeName,
                        folderPath, name);

                HippoBinaryNodeUtils.extractTextFromBinariesAndSaveHippoTextsUnderHandlePath(
                        importTask.getDocumentManager().getSession(), updatedPath);

                record.setSucceeded(true);
            } catch (Exception e) {
                procLogger.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    importTask.endRecord();
                    result.addItem(recordToResultItem(record));
                    result.incrementTotalBinaryCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededBinaryCount();
                    } else {
                        result.incrementFailedBinaryCount();
                    }
                    if (processStatus != null) {
                        // the remaining 5% for cleaning paths to convert those to uuids.
                        processStatus.setProgress(0.95 * ((double) batchCount) / ((double) jsonFiles.length));
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    importTask.getDocumentManager().getSession().save();
                    importTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        importTask.getDocumentManager().getSession().save();
        importTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }

    private int importDocuments(Logger procLogger, ProcessStatus processStatus, FileObject[] jsonFiles,
            ExecutionParams params, FileObject baseFolder, WorkflowDocumentVariantImportTask importTask, Result result,
            int batchCount) throws Exception {
        final String baseFolderUrlPrefix = baseFolder.getURL().toString();
        final AntPathMatcher pathMatcher = new AntPathMatcher();

        for (FileObject file : jsonFiles) {
            if (isStopRequested(baseFolder)) {
                procLogger.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(),
                        STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentNode contentNode = importTask.readContentNodeFromJsonFile(file);

            String primaryTypeName = contentNode.getPrimaryType();
            String path = contentNode.getProperty("jcr:path").getValue();

            if (!isDocumentPathIncluded(pathMatcher, params, path)) {
                continue;
            }

            if (!HippoNodeUtils.isDocumentPath(path)) {
                continue;
            }

            ContentMigrationRecord record = null;

            try {
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH,
                        baseFolderUrlPrefix);

                record = importTask.beginRecord("", path);
                record.setAttribute("file", file.getName().getPath());
                record.setProcessed(true);

                String locale = (contentNode.hasProperty("hippotranslation:locale"))
                        ? contentNode.getProperty("hippotranslation:locale").getValue()
                        : null;
                String localizedName = contentNode.getProperty("jcr:localizedName").getValue();

                applyTagContentProperties(contentNode, params.getDocumentTags());

                String updatedPath = importTask.createOrUpdateDocumentFromVariantContentNode(contentNode,
                        primaryTypeName, path, locale, localizedName);

                boolean isToPublish = ExecutionParams.PUBLISH_ON_IMPORT_ALL.equals(params.getPublishOnImport());

                if (!isToPublish && ExecutionParams.PUBLISH_ON_IMPORT_LIVE.equals(params.getPublishOnImport())) {
                    isToPublish = ContentNodeUtils.containsStringValueInProperty(contentNode,
                            HippoNodeType.HIPPO_AVAILABILITY, "live");
                }

                if (isToPublish) {
                    importTask.getDocumentManager().depublishDocument(updatedPath);
                    importTask.getDocumentManager().publishDocument(updatedPath);
                }

                record.setSucceeded(true);
            } catch (Exception e) {
                procLogger.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    importTask.endRecord();
                    result.addItem(recordToResultItem(record));
                    result.incrementTotalDocumentCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededDocumentCount();
                    } else {
                        result.incrementFailedDocumentCount();
                    }
                    if (processStatus != null) {
                        // the remaining 5% for cleaning paths to convert those to uuids.
                        processStatus.setProgress(0.95 * ((double) batchCount) / ((double) jsonFiles.length));
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    importTask.getDocumentManager().getSession().save();
                    importTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        importTask.getDocumentManager().getSession().save();
        importTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }

    private int cleanMirrorDocbaseValues(Logger procLogger, ProcessStatus processStatus, Session session,
            ExecutionParams params, Result result, int batchCount) throws Exception {
        Set<String> mirrorNodePaths = getQueriedNodePaths(session,
                "//element(*)[jcr:like(@hippo:docbase,'/content/%')]", Query.XPATH);
        session.refresh(false);

        for (String mirrorNodePath : mirrorNodePaths) {
            try {
                if (!session.nodeExists(mirrorNodePath)) {
                    continue;
                }

                Node mirrorNode = session.getNode(mirrorNodePath);
                String docbasePath = mirrorNode.getProperty("hippo:docbase").getString();

                if (StringUtils.startsWith(docbasePath, "/") && session.nodeExists(docbasePath)) {
                    JcrUtils.ensureIsCheckedOut(mirrorNode);
                    String docbase = session.getNode(docbasePath).getIdentifier();
                    mirrorNode.setProperty("hippo:docbase", docbase);
                }
            } catch (Exception e) {
                String message = "Failed to clean mirror docbase value at " + mirrorNodePath + ". " + e;
                result.addError(message);
                procLogger.error("Failed to clean mirror docbase value at {}.", mirrorNodePath, e);
            } finally {
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    session.save();
                    session.refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        session.save();
        session.refresh(false);

        return batchCount;
    }

    private int cleanAllDocbaseFieldValues(Logger procLogger, ProcessStatus processStatus, Session session,
            ExecutionParams params, Result result, int batchCount) throws Exception {
        Set<String> docbasePropNames = params.getDocbasePropNames();

        if (CollectionUtils.isEmpty(docbasePropNames)) {
            return batchCount;
        }

        for (String docbasePropName : docbasePropNames) {
            if (StringUtils.isNotBlank(docbasePropName)) {
                batchCount = cleanSingleDocbaseFieldValues(procLogger, processStatus, session, params,
                        StringUtils.trim(docbasePropName), result, batchCount);
            }
        }

        return batchCount;
    }

    private int cleanSingleDocbaseFieldValues(Logger procLogger, ProcessStatus processStatus, Session session,
            ExecutionParams params, String docbasePropName, Result result, int batchCount) throws Exception {
        Set<String> nodePaths = getQueriedNodePaths(session,
                "//element(*)[jcr:like(@" + docbasePropName + ",'/content/%')]", Query.XPATH);

        session.refresh(false);

        for (String nodePath : nodePaths) {
            try {
                if (!session.nodeExists(nodePath)) {
                    continue;
                }

                Node node = session.getNode(nodePath);

                if (!node.hasProperty(docbasePropName)) {
                    continue;
                }

                Property docbaseProp = node.getProperty(docbasePropName);

                if (docbaseProp.isMultiple()) {
                    String[] docbasePaths = JcrUtils.getMultipleStringProperty(node, docbasePropName, null);
                    if (ArrayUtils.isNotEmpty(docbasePaths)) {
                        boolean updated = false;
                        for (int i = 0; i < docbasePaths.length; i++) {
                            String docbasePath = docbasePaths[i];
                            if (StringUtils.startsWith(docbasePath, "/") && session.nodeExists(docbasePath)) {
                                String docbase = session.getNode(docbasePath).getIdentifier();
                                docbasePaths[i] = docbase;
                                updated = true;
                            }
                        }
                        if (updated) {
                            JcrUtils.ensureIsCheckedOut(node);
                            node.setProperty(docbasePropName, docbasePaths);
                        }
                    }
                } else {
                    String docbasePath = JcrUtils.getStringProperty(node, docbasePropName, null);
                    if (StringUtils.startsWith(docbasePath, "/") && session.nodeExists(docbasePath)) {
                        JcrUtils.ensureIsCheckedOut(node);
                        String docbase = session.getNode(docbasePath).getIdentifier();
                        node.setProperty(docbasePropName, docbase);
                    }
                }
            } catch (Exception e) {
                String message = "Failed to clean mirror docbase value at " + nodePath + "/@" + docbasePropName + ". "
                        + e;
                result.addError(message);
                procLogger.error("Failed to clean mirror docbase value at {}/@{}.", nodePath, docbasePropName, e);
            } finally {
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    session.save();
                    session.refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        session.save();
        session.refresh(false);

        return batchCount;
    }
}
