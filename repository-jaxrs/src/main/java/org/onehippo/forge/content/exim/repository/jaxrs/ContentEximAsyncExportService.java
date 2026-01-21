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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.onehippo.forge.content.exim.repository.jaxrs.param.AsyncResponse;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ErrorResponse;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
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
            ExecutionParams params = parseExecutionParameters(paramsAttachment, paramsJsonParam, batchSizeParam,
                    throttleParam, publishOnImportParam, dataUrlSizeThresholdParam, docbasePropNamesParam,
                    documentTagsParam, binaryTagsParam);
            processStatus.setExecutionParams(params);

            // Submit export task to executor
            AsyncExportTask exportTask = new AsyncExportTask(processStatus, exportFilePath, params, fileManager,
                    exportService, getProcessMonitor());
            executorService.submit(exportTask);

            // Return response with process ID
            AsyncResponse response = new AsyncResponse();
            response.setProcessId(processStatus.getId());
            response.setStatusUrl("/exim/ps/" + processStatus.getId());
            response.setDownloadUrl("/exim/export-async/" + processStatus.getId());

            return Response.accepted().entity(response).build();

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
            String errorMessage = "Failed to initiate export operation. Please check your parameters and try again.";
            ErrorResponse errorResponse = new ErrorResponse("EXPORT_INIT_FAILED", errorMessage);
            return Response.serverError().entity(errorResponse).build();
        }
    }

    /**
     * Downloads a completed export file.
     * Returns 202 Accepted if export is still in progress.
     */
    @Path("/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadExport(@PathParam("processId") String processId,
            @Multipart(value = "deleteAfterDownload", required = false) String deleteAfterDownloadParam) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            ErrorResponse errorResponse = new ErrorResponse("PROCESS_NOT_FOUND", "Process not found");
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        // If process is still running, return 202 Accepted
        if (processStatus.getStatus() == ProcessStatus.Status.RUNNING) {
            return Response.accepted().build();
        }

        // If process failed, return error
        if (processStatus.getStatus() == ProcessStatus.Status.FAILED) {
            String errorMessage = "Export operation failed. Please check the process logs for details.";
            ErrorResponse errorResponse = new ErrorResponse("failed", "EXPORT_FAILED", errorMessage);
            return Response.serverError().entity(errorResponse).build();
        }

        // If process was cancelled, return error
        if (processStatus.getStatus() == ProcessStatus.Status.CANCELLED) {
            ErrorResponse errorResponse = new ErrorResponse("cancelled", "EXPORT_CANCELLED", "Export was cancelled");
            return Response.status(Response.Status.GONE).entity(errorResponse).build();
        }

        String filePath = processStatus.getExportFilePath();
        if (StringUtils.isBlank(filePath)) {
            ErrorResponse errorResponse = new ErrorResponse("FILE_PATH_UNAVAILABLE", "Export file path not available");
            return Response.serverError().entity(errorResponse).build();
        }

        try {
            File exportFile = fileManager.getExportFile(filePath);
            boolean deleteAfterDownload = StringUtils.equalsIgnoreCase(deleteAfterDownloadParam, "true");

            // Stream the file back to client
            String fileName = String.format("exim-export-%s.zip", processId);
            StreamingFileOutput fileOutput = new StreamingFileOutput(exportFile, deleteAfterDownload ? filePath : null);

            return Response.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Length", exportFile.length())
                    .entity(fileOutput)
                    .build();

        } catch (FileNotFoundException e) {
            log.warn("Export file not found for process {}: {}", processId, filePath);
            ErrorResponse errorResponse = new ErrorResponse("FILE_NOT_FOUND", "Export file not found");
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        } catch (IOException e) {
            log.error("Failed to retrieve export file for process {}", processId, e);
            ErrorResponse errorResponse = new ErrorResponse("FILE_RETRIEVAL_FAILED", "Failed to retrieve export file");
            return Response.serverError().entity(errorResponse).build();
        }
    }

    /**
     * Cancels an in-progress export operation.
     */
    @Path("/{processId}")
    @DELETE
    public Response cancelExport(@PathParam("processId") String processId) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            ErrorResponse errorResponse = new ErrorResponse("PROCESS_NOT_FOUND", "Process not found");
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        if (processStatus.getStatus() != ProcessStatus.Status.RUNNING) {
            ErrorResponse errorResponse = new ErrorResponse("error", "INVALID_STATE",
                    "Process is " + processStatus.getStatus() + ", cannot cancel");
            return Response.status(Response.Status.CONFLICT).entity(errorResponse).build();
        }

        processStatus.requestCancellation();
        return Response.ok().build();
    }

    /**
     * Parses execution parameters from request attachments and form parameters.
     */
    private ExecutionParams parseExecutionParameters(
            Attachment paramsAttachment,
            String paramsJsonParam,
            String batchSizeParam,
            String throttleParam,
            String publishOnImportParam,
            String dataUrlSizeThresholdParam,
            String docbasePropNamesParam,
            String documentTagsParam,
            String binaryTagsParam) throws IOException {

        ExecutionParams params = new ExecutionParams();

        if (paramsAttachment != null) {
            final String json = attachmentToString(paramsAttachment, "UTF-8");
            if (StringUtils.isNotBlank(json)) {
                params = getObjectMapper().readValue(json, ExecutionParams.class);
            }
        } else if (StringUtils.isNotBlank(paramsJsonParam)) {
            params = getObjectMapper().readValue(paramsJsonParam, ExecutionParams.class);
        }

        overrideExecutionParamsByParameters(params, batchSizeParam, throttleParam, publishOnImportParam,
                dataUrlSizeThresholdParam, docbasePropNamesParam, documentTagsParam, binaryTagsParam);

        return params;
    }

    /**
     * Wrapper for streaming file output with optional deletion after write.
     */
    static class StreamingFileOutput implements jakarta.ws.rs.core.StreamingOutput {

        private static final Logger log = LoggerFactory.getLogger(StreamingFileOutput.class);

        private final File file;
        private final String filePathToDelete;

        StreamingFileOutput(File file, String filePathToDelete) {
            this.file = file;
            this.filePathToDelete = filePathToDelete;
        }

        @Override
        public void write(java.io.OutputStream output) throws IOException {
            try {
                org.apache.commons.io.FileUtils.copyFile(file, output);
            } finally {
                IOUtils.closeQuietly(output);
                if (filePathToDelete != null) {
                    try {
                        org.apache.commons.io.FileUtils.forceDelete(new File(filePathToDelete));
                    } catch (IOException e) {
                        log.warn("Failed to delete file after download: {}", filePathToDelete, e);
                    }
                }
            }
        }
    }

}
