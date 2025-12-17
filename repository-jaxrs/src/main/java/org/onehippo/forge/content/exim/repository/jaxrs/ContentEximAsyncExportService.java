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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;

import javax.jcr.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ZipCompressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous Content Export JAX-RS Service.
 * Provides two-phase export: initiate export and download results separately.
 */
@Path("/export-async")
public class ContentEximAsyncExportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximAsyncExportService.class);

    private ProcessFileManager fileManager;
    private ExecutorService executorService;
    private ContentEximExportService exportService;

    public void setFileManager(ProcessFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setExportService(ContentEximExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Initiates an asynchronous export operation.
     * Returns immediately with a process ID and status URL.
     */
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response initiateExportAsync(@Context SecurityContext securityContext, @Context HttpServletRequest request,
            @Multipart(value = "batchSize", required = false) String batchSizeParam,
            @Multipart(value = "throttle", required = false) String throttleParam,
            @Multipart(value = "publishOnImport", required = false) String publishOnImportParam,
            @Multipart(value = "dataUrlSizeThreshold", required = false) String dataUrlSizeThresholdParam,
            @Multipart(value = "docbasePropNames", required = false) String docbasePropNamesParam,
            @Multipart(value = "documentTags", required = false) String documentTagsParam,
            @Multipart(value = "binaryTags", required = false) String binaryTagsParam,
            @Multipart(value = "paramsJson", required = false) String paramsJsonParam,
            @Multipart(value = "params", required = false) Attachment paramsAttachment) {

        ProcessStatus processStatus = null;
        String exportFilePath = null;

        try {
            // Start process and create export file
            processStatus = getProcessMonitor().startProcess();
            fillProcessStatusByRequestInfo(processStatus, securityContext, request);
            exportFilePath = fileManager.createExportFile(processStatus.getId());
            processStatus.setExportFilePath(exportFilePath);

            // Parse execution parameters
            ExecutionParams params = new ExecutionParams();
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
            processStatus.setExecutionParams(params);

            // Submit export task to executor
            AsyncExportTask exportTask = new AsyncExportTask(processStatus, exportFilePath, params);
            executorService.submit(exportTask);

            // Return response with process ID
            String responseJson = String.format(
                    "{\"processId\": %d, \"statusUrl\": \"/exim/ps/%d\", \"downloadUrl\": \"/exim/export-async/%d\"}",
                    processStatus.getId(), processStatus.getId(), processStatus.getId());

            return Response.accepted().entity(responseJson).build();

        } catch (Exception e) {
            log.error("Failed to initiate async export", e);
            if (processStatus != null) {
                processStatus.setStatus(ProcessStatus.Status.FAILED);
                processStatus.setErrorMessage(e.getMessage());
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());
                getProcessMonitor().stopProcess(processStatus);
            }
            if (exportFilePath != null) {
                try {
                    fileManager.deleteExportFile(exportFilePath);
                } catch (IOException ioe) {
                    log.warn("Failed to cleanup export file after error", ioe);
                }
            }
            return Response.serverError().entity(String.format("{\"error\": \"%s\"}", e.getMessage())).build();
        }
    }

    /**
     * Downloads a completed export file.
     * Returns 202 Accepted if export is still in progress.
     */
    @Path("/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadExport(@PathParam("processId") long processId,
            @Multipart(value = "deleteAfterDownload", required = false) String deleteAfterDownloadParam) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Process not found").build();
        }

        // If process is still running, return 202 Accepted
        if (processStatus.getStatus() == ProcessStatus.Status.RUNNING) {
            return Response.accepted().entity("Export in progress").build();
        }

        // If process failed, return error
        if (processStatus.getStatus() == ProcessStatus.Status.FAILED) {
            return Response.serverError().entity(String.format("Export failed: %s", processStatus.getErrorMessage()))
                    .build();
        }

        // If process was cancelled, return error
        if (processStatus.getStatus() == ProcessStatus.Status.CANCELLED) {
            return Response.status(Response.Status.GONE).entity("Export was cancelled").build();
        }

        String filePath = processStatus.getExportFilePath();
        if (StringUtils.isBlank(filePath)) {
            return Response.serverError().entity("Export file path not available").build();
        }

        try {
            File exportFile = fileManager.getExportFile(filePath);
            boolean deleteAfterDownload = StringUtils.equalsIgnoreCase(deleteAfterDownloadParam, "true");

            // Stream the file back to client
            String fileName = String.format("exim-export-%d.zip", processId);
            StreamingFileOutput fileOutput = new StreamingFileOutput(exportFile, deleteAfterDownload ? filePath : null);

            return Response.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Length", exportFile.length())
                    .entity(fileOutput)
                    .build();

        } catch (FileNotFoundException e) {
            log.warn("Export file not found for process {}: {}", processId, filePath);
            return Response.status(Response.Status.NOT_FOUND).entity("Export file not found").build();
        } catch (IOException e) {
            log.error("Failed to retrieve export file for process {}", processId, e);
            return Response.serverError().entity("Failed to retrieve export file").build();
        }
    }

    /**
     * Cancels an in-progress export operation.
     */
    @Path("/{processId}")
    @DELETE
    public Response cancelExport(@PathParam("processId") long processId) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Process not found").build();
        }

        if (processStatus.getStatus() != ProcessStatus.Status.RUNNING) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(String.format("Process is %s, cannot cancel", processStatus.getStatus())).build();
        }

        processStatus.requestCancellation();
        return Response.ok().entity("{\"message\": \"Cancellation requested\"}").build();
    }

    /**
     * Task for executing export asynchronously.
     */
    private class AsyncExportTask implements Runnable {

        private final ProcessStatus processStatus;
        private final String exportFilePath;
        private final ExecutionParams params;

        AsyncExportTask(ProcessStatus processStatus, String exportFilePath, ExecutionParams params) {
            this.processStatus = processStatus;
            this.exportFilePath = exportFilePath;
            this.params = params;
        }

        @Override
        public void run() {
            Logger procLogger = log;
            File tempLogFile = null;
            PrintStream tempLogOut = null;
            OutputStream fileOutput = null;

            try {
                // Create temporary log file
                tempLogFile = File.createTempFile("exim-export-async", ".log");
                tempLogOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(tempLogFile)));
                procLogger = createTeeLogger(log, tempLogOut);

                procLogger.info("AsyncExportTask started for process {}", processStatus.getId());

                // Call the synchronous export service to do the actual work
                // This reuses the existing export logic
                // For now, we'll document that this should delegate to the export service
                performExport(procLogger);

                processStatus.setStatus(ProcessStatus.Status.COMPLETED);
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());
                procLogger.info("AsyncExportTask completed successfully for process {}", processStatus.getId());

            } catch (Exception e) {
                procLogger.error("AsyncExportTask failed for process {}", processStatus.getId(), e);
                processStatus.setStatus(ProcessStatus.Status.FAILED);
                processStatus.setErrorMessage(e.getMessage());
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());

                // Cleanup failed export file
                try {
                    fileManager.deleteExportFile(exportFilePath);
                } catch (IOException ioe) {
                    procLogger.warn("Failed to cleanup export file after error", ioe);
                }

            } finally {
                procLogger.info("AsyncExportTask finally block for process {}", processStatus.getId());

                if (tempLogOut != null) {
                    IOUtils.closeQuietly(tempLogOut);
                }

                if (tempLogFile != null) {
                    try {
                        tempLogFile.delete();
                    } catch (Exception e) {
                        log.error("Failed to delete temporary log file", e);
                    }
                }

                getProcessMonitor().stopProcess(processStatus);
            }
        }

        private void performExport(Logger procLogger) throws Exception {
            File baseFolder = null;
            File zipFile = new File(exportFilePath);
            Session session = null;

            try {
                // Create temporary folder for content
                baseFolder = java.nio.file.Files.createTempDirectory("exim-async-export").toFile();
                procLogger.info("Starting async export to base folder: {}", baseFolder);

                // Create JCR session
                session = exportService.createSession();

                // Perform core export
                ContentEximExportService.ExportCoreResult exportResult =
                        exportService.performExportCore(procLogger, processStatus, baseFolder, session, params);

                session.logout();
                session = null;

                procLogger.info("Core export completed, creating ZIP file");

                // Create ZIP file from exported content
                createZipFile(procLogger, baseFolder, zipFile, exportResult);

                procLogger.info("ZIP file created successfully: {}", zipFile.getAbsolutePath());

            } finally {
                if (session != null) {
                    try {
                        session.logout();
                    } catch (Exception e) {
                        procLogger.error("Failed to logout session", e);
                    }
                }
                if (baseFolder != null && baseFolder.exists()) {
                    try {
                        FileUtils.deleteDirectory(baseFolder);
                    } catch (IOException e) {
                        procLogger.error("Failed to delete base folder: {}", baseFolder, e);
                    }
                }
            }
        }

        private void createZipFile(Logger procLogger, File baseFolder, File zipFile,
                ContentEximExportService.ExportCoreResult exportResult) throws Exception {

            try (ZipArchiveOutputStream zipOutput = new ZipArchiveOutputStream(new FileOutputStream(zipFile))) {
                // Enable Unicode extra fields for non-ASCII filenames (FORGE-448)
                zipOutput.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

                // Add execution log
                String executionLog = String.format("Export process %d completed successfully\n", processStatus.getId());
                ZipCompressUtils.addEntryToZip(AbstractContentEximService.EXIM_EXECUTION_LOG_REL_PATH,
                        executionLog, "UTF-8", zipOutput);

                // Add summaries
                ZipCompressUtils.addEntryToZip(AbstractContentEximService.EXIM_SUMMARY_BINARIES_LOG_REL_PATH,
                        exportResult.binaryExportTask.getSummary(), "UTF-8", zipOutput);
                ZipCompressUtils.addEntryToZip(AbstractContentEximService.EXIM_SUMMARY_DOCUMENTS_LOG_REL_PATH,
                        exportResult.documentExportTask.getSummary(), "UTF-8", zipOutput);

                // Add content files
                ZipCompressUtils.addFileEntriesInFolderToZip(baseFolder, "", zipOutput);

                zipOutput.finish();
            }
        }
    }

    /**
     * Wrapper for streaming file output with optional deletion after write.
     */
    private static class StreamingFileOutput implements jakarta.ws.rs.core.StreamingOutput {

        private final File file;
        private final String filePathToDelete;

        StreamingFileOutput(File file, String filePathToDelete) {
            this.file = file;
            this.filePathToDelete = filePathToDelete;
        }

        @Override
        public void write(OutputStream output) throws IOException {
            try {
                FileUtils.copyFile(file, output);
            } finally {
                IOUtils.closeQuietly(output);
                if (filePathToDelete != null) {
                    try {
                        FileUtils.forceDelete(new File(filePathToDelete));
                    } catch (IOException e) {
                        log.warn("Failed to delete file after download: {}", filePathToDelete, e);
                    }
                }
            }
        }
    }
}
