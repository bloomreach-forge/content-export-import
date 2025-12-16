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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.impl.DefaultBinaryExportTask;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentManagerImpl;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentVariantExportTask;
import org.onehippo.forge.content.exim.core.util.AntPathMatcher;
import org.onehippo.forge.content.exim.core.util.ContentNodeUtils;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ResultItemSetCollector;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ZipCompressUtils;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content-EXIM Export JAX-RS Service.
 */
@Path("/export")
public class ContentEximExportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximExportService.class);

    public ContentEximExportService() {
        super();
    }

    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @POST
    public Response exportContentToZip(@Context SecurityContext securityContext, @Context HttpServletRequest request,
            @Multipart(value = "batchSize", required = false) String batchSizeParam,
            @Multipart(value = "throttle", required = false) String throttleParam,
            @Multipart(value = "publishOnImport", required = false) String publishOnImportParam,
            @Multipart(value = "dataUrlSizeThreshold", required = false) String dataUrlSizeThresholdParam,
            @Multipart(value = "docbasePropNames", required = false) String docbasePropNamesParam,
            @Multipart(value = "documentTags", required = false) String documentTagsParam,
            @Multipart(value = "binaryTags", required = false) String binaryTagsParam,
            @Multipart(value = "paramsJson", required = false) String paramsJsonParam,
            @Multipart(value = "params", required = false) Attachment paramsAttachment) {

        Logger procLogger = log;

        File tempLogFile = null;
        PrintStream tempLogOut = null;
        File baseFolder = null;
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

            baseFolder = Files.createTempDirectory(TEMP_PREFIX).toFile();
            procLogger.info("ContentEximService#exportContentToZip begins at {}.", baseFolder);

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

            session = createSession();
            Result result = ResultItemSetCollector.collectItemsFromExecutionParams(session, params);
            session.refresh(false);

            FileObject baseFolderObject = VFS.getManager().resolveFile(baseFolder.toURI());
            FileObject attachmentsFolderObject = baseFolderObject.resolveFile(BINARY_ATTACHMENT_REL_PATH);

            DocumentManager documentManager = new WorkflowDocumentManagerImpl(session);

            final WorkflowDocumentVariantExportTask documentExportTask = new WorkflowDocumentVariantExportTask(
                    documentManager);
            documentExportTask.setLogger(log);
            documentExportTask.setBinaryValueFileFolder(attachmentsFolderObject);
            documentExportTask.setDataUrlSizeThreashold(params.getDataUrlSizeThreshold());

            final DefaultBinaryExportTask binaryExportTask = new DefaultBinaryExportTask(documentManager);
            binaryExportTask.setLogger(log);
            binaryExportTask.setBinaryValueFileFolder(attachmentsFolderObject);
            binaryExportTask.setDataUrlSizeThreashold(params.getDataUrlSizeThreshold());

            int batchCount = 0;

            Set<String> referredNodePaths = new LinkedHashSet<>();

            try {
                documentExportTask.start();
                batchCount = exportDocuments(procLogger, processStatus, params, documentExportTask, result, batchCount,
                        baseFolderObject, referredNodePaths);
            } finally {
                documentExportTask.stop();
            }

            if (!referredNodePaths.isEmpty()) {
                ResultItemSetCollector.fillResultItemsForNodePaths(session, referredNodePaths, true, null, result);
                session.refresh(false);
            }

            try {
                binaryExportTask.start();
                batchCount = exportBinaries(procLogger, processStatus, params, binaryExportTask, result, batchCount,
                        baseFolderObject);
            } finally {
                binaryExportTask.stop();
            }

            session.logout();
            session = null;

            procLogger.info("ContentEximService#exportContentToZip ends.");

            tempLogOut.close();
            tempLogOut = null;
            procLogger = log;

            final String tempLogOutString = FileUtils.readFileToString(tempLogFile, "UTF-8");
            final File zipBaseFolder = baseFolder;

            final StreamingOutput entity = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    ZipArchiveOutputStream zipOutput = null;
                    try {
                        zipOutput = new ZipArchiveOutputStream(output);
                        // FORGE-448: Enable Unicode extra fields for proper handling of non-ASCII filenames (e.g., Cyrillic characters)
                        // This ensures that filenames with Unicode characters are correctly preserved in the ZIP archive
                        // without being mangled or converted to question marks during extraction
                        zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

                        ZipCompressUtils.addEntryToZip(EXIM_EXECUTION_LOG_REL_PATH, tempLogOutString, "UTF-8",
                                zipOutput);
                        ZipCompressUtils.addEntryToZip(EXIM_SUMMARY_BINARIES_LOG_REL_PATH,
                                binaryExportTask.getSummary(), "UTF-8", zipOutput);
                        ZipCompressUtils.addEntryToZip(EXIM_SUMMARY_DOCUMENTS_LOG_REL_PATH,
                                documentExportTask.getSummary(), "UTF-8", zipOutput);
                        ZipCompressUtils.addFileEntriesInFolderToZip(zipBaseFolder, "", zipOutput);
                    } finally {
                        zipOutput.finish();
                        IOUtils.closeQuietly(zipOutput);
                        FileUtils.deleteDirectory(zipBaseFolder);
                    }
                }
            };

            String fileName = "exim-export-" + DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd-HHmmss")
                    + ".zip";
            return Response.ok().header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .entity(entity).build();
        } catch (Exception e) {
            procLogger.error("Failed to export content.", e);
            if (baseFolder != null) {
                try {
                    FileUtils.deleteDirectory(baseFolder);
                } catch (Exception ioe) {
                    procLogger.error("Failed to delete the temporary folder at {}", baseFolder.getPath(), e);
                }
            }
            final String message = new StringBuilder().append(e.getMessage()).append("\r\n").toString();
            return Response.serverError().entity(message).build();
        } finally {
            procLogger.info("ContentEximService#exportContentToZip finally ends.");

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
    }

    private int exportBinaries(Logger procLogger, ProcessStatus processStatus, ExecutionParams params,
            DefaultBinaryExportTask exportTask, Result result, int batchCount, FileObject baseFolder) throws Exception {
        final String baseFolderUrlPrefix = baseFolder.getURL().toString() + "/";
        final AntPathMatcher pathMatcher = new AntPathMatcher();

        for (ResultItem item : result.getItems()) {
            if (isStopRequested(baseFolder)) {
                procLogger.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(),
                        STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentMigrationRecord record = null;

            try {
                String handlePath = item.getPath();

                if (!isBinaryPathIncluded(pathMatcher, params, handlePath)) {
                    continue;
                }

                if (!HippoNodeUtils.isBinaryPath(handlePath)) {
                    continue;
                }

                if (!exportTask.getDocumentManager().getSession().nodeExists(handlePath)) {
                    continue;
                }

                Node handle = exportTask.getDocumentManager().getSession().getNode(handlePath);
                Node variant = HippoNodeUtils.getFirstVariantNode(handle);

                if (variant == null) {
                    continue;
                }

                String variantPath = variant.getPath();
                record = exportTask.beginRecord(variant.getIdentifier(), variantPath);

                ContentNode contentNode = exportTask.exportBinarySetToContentNode(variant);
                record.setProcessed(true);

                ContentNodeUtils.replaceDocbasesByPaths(exportTask.getDocumentManager().getSession(), contentNode,
                        ContentNodeUtils.MIRROR_DOCBASES_XPATH);

                Set<String> docbasePropNames = params.getDocbasePropNames();
                if (CollectionUtils.isNotEmpty(docbasePropNames)) {
                    for (String docbasePropName : docbasePropNames) {
                        ContentNodeUtils.replaceDocbasePropertiesByPaths(exportTask.getDocumentManager().getSession(),
                                contentNode, "properties[@itemName='" + docbasePropName + "']");
                    }
                }

                ContentNodeUtils.removeUrlPrefixInJcrDataValues(contentNode, baseFolderUrlPrefix);

                applyTagContentProperties(contentNode, params.getBinaryTags());

                String relPath = StringUtils.removeStart(ContentPathUtils.removeIndexNotationInNodePath(variantPath),
                        "/");
                FileObject file = baseFolder.resolveFile(relPath + ".json");
                record.setAttribute("file", file.getName().getPath());
                exportTask.writeContentNodeToJsonFile(contentNode, file);

                procLogger.debug("Exported document from {} to {}.", handlePath, file.getName().getPath());
                record.setSucceeded(true);
            } catch (Exception e) {
                procLogger.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    exportTask.endRecord();
                    result.incrementTotalBinaryCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededBinaryCount();
                    } else {
                        result.incrementFailedBinaryCount();
                    }
                    if (processStatus != null) {
                        processStatus.setProgress(result.getProgress());
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    exportTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        exportTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }

    private int exportDocuments(Logger procLogger, ProcessStatus processStatus, ExecutionParams params,
            WorkflowDocumentVariantExportTask exportTask, Result result, int batchCount, FileObject baseFolder,
            Set<String> referredBinaryPaths) throws Exception {
        final String baseFolderUrlPrefix = baseFolder.getURL().toString() + "/";
        final AntPathMatcher pathMatcher = new AntPathMatcher();

        for (ResultItem item : result.getItems()) {
            if (isStopRequested(baseFolder)) {
                procLogger.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(),
                        STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentMigrationRecord record = null;

            try {
                String handlePath = item.getPath();

                if (!isDocumentPathIncluded(pathMatcher, params, handlePath)) {
                    continue;
                }

                if (!HippoNodeUtils.isDocumentPath(handlePath)) {
                    continue;
                }

                if (!exportTask.getDocumentManager().getSession().nodeExists(handlePath)) {
                    continue;
                }

                Node handle = exportTask.getDocumentManager().getSession().getNode(handlePath);
                Map<String, Node> variantsMap = HippoNodeUtils.getDocumentVariantsMap(handle);
                Node variant = variantsMap.get(HippoStdNodeType.PUBLISHED);
                if (variant == null) {
                    variant = variantsMap.get(HippoStdNodeType.UNPUBLISHED);
                }

                if (variant == null) {
                    continue;
                }

                String variantPath = variant.getPath();
                record = exportTask.beginRecord(variant.getIdentifier(), variantPath);

                Document document = new Document(variant.getIdentifier());
                ContentNode contentNode = exportTask.exportVariantToContentNode(document);
                record.setProcessed(true);

                ContentNodeUtils.replaceDocbasesByPaths(exportTask.getDocumentManager().getSession(), contentNode,
                        ContentNodeUtils.MIRROR_DOCBASES_XPATH, referredBinaryPaths);

                Set<String> docbasePropNames = params.getDocbasePropNames();
                if (CollectionUtils.isNotEmpty(docbasePropNames)) {
                    for (String docbasePropName : docbasePropNames) {
                        ContentNodeUtils.replaceDocbasePropertiesByPaths(exportTask.getDocumentManager().getSession(),
                                contentNode, "properties[@itemName='" + docbasePropName + "']");
                    }
                }

                ContentNodeUtils.removeUrlPrefixInJcrDataValues(contentNode, baseFolderUrlPrefix);

                applyTagContentProperties(contentNode, params.getDocumentTags());

                String relPath = StringUtils.removeStart(ContentPathUtils.removeIndexNotationInNodePath(variantPath),
                        "/");
                FileObject file = baseFolder.resolveFile(relPath + ".json");
                record.setAttribute("file", file.getName().getPath());

                exportTask.writeContentNodeToJsonFile(contentNode, file);
                procLogger.debug("Exported document from {} to {}.", handlePath, file.getName().getPath());
                record.setSucceeded(true);
            } catch (Exception e) {
                procLogger.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    exportTask.endRecord();
                    result.incrementTotalDocumentCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededDocumentCount();
                    } else {
                        result.incrementFailedDocumentCount();
                    }
                    if (processStatus != null) {
                        processStatus.setProgress(result.getProgress());
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    exportTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThrottle() > 0) {
                        Thread.sleep(params.getThrottle());
                    }
                }
            }
        }

        exportTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }
}
