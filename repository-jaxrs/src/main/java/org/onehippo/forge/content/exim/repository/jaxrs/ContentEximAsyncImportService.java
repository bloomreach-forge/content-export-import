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
import org.onehippo.forge.content.exim.repository.jaxrs.param.ImportResultsResponse;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ProcessProgressResponse;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous Content Import JAX-RS Service.
 * Provides two-phase import: upload file and process asynchronously.
 */
@Path("/import-async")
public class ContentEximAsyncImportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximAsyncImportService.class);

    private ProcessFileManager fileManager;
    private ExecutorService executorService;
    private ContentEximImportService importService;

    public void setFileManager(ProcessFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setImportService(ContentEximImportService importService) {
        this.importService = importService;
    }

    /**
     * Initiates an asynchronous import operation by uploading a package file.
     * Returns immediately with a process ID and status URL.
     */
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response initiateImportAsync(@Context SecurityContext securityContext, @Context HttpServletRequest request,
            @Multipart(value = "batchSize", required = false) String batchSizeParam,
            @Multipart(value = "throttle", required = false) String throttleParam,
            @Multipart(value = "publishOnImport", required = false) String publishOnImportParam,
            @Multipart(value = "dataUrlSizeThreshold", required = false) String dataUrlSizeThresholdParam,
            @Multipart(value = "docbasePropNames", required = false) String docbasePropNamesParam,
            @Multipart(value = "package", required = true) Attachment packageAttachment,
            @Multipart(value = "paramsJson", required = false) String paramsJsonParam,
            @Multipart(value = "params", required = false) Attachment paramsAttachment) {

        ProcessStatus processStatus = null;
        String importFilePath = null;

        try {
            if (packageAttachment == null) {
                ErrorResponse errorResponse = new ErrorResponse("PACKAGE_REQUIRED", "Package attachment is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }

            // Start process and create import file
            processStatus = getProcessMonitor().startProcess();
            fillProcessStatusByRequestInfo(processStatus, securityContext, request);
            importFilePath = fileManager.createImportFile(processStatus.getId());
            processStatus.setImportFilePath(importFilePath);

            // Write uploaded package to import file
            writePackageToFile(packageAttachment, importFilePath);

            // Parse execution parameters
            ExecutionParams params = parseExecutionParameters(paramsAttachment, paramsJsonParam, batchSizeParam,
                    throttleParam, publishOnImportParam, dataUrlSizeThresholdParam, docbasePropNamesParam);
            processStatus.setExecutionParams(params);

            // Submit import task to executor
            AsyncImportTask importTask = new AsyncImportTask(processStatus, importFilePath, params, fileManager,
                    importService, getProcessMonitor());
            executorService.submit(importTask);

            // Return response with process ID
            AsyncResponse response = new AsyncResponse();
            response.setProcessId(processStatus.getId());
            response.setStatusUrl("/exim/ps/" + processStatus.getId());
            response.setResultsUrl("/exim/import-async/" + processStatus.getId());

            return Response.accepted().entity(response).build();

        } catch (Exception e) {
            log.error("Failed to initiate async import", e);
            if (processStatus != null) {
                processStatus.setStatus(ProcessStatus.Status.FAILED);
                processStatus.setErrorMessage(e.getMessage());
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());
                getProcessMonitor().stopProcess(processStatus);
            }
            if (importFilePath != null) {
                try {
                    fileManager.deleteImportFile(importFilePath);
                } catch (IOException ioe) {
                    log.warn("Failed to cleanup import file after error", ioe);
                }
            }
            String errorMessage = "Failed to initiate import operation. Please verify your package file and parameters.";
            ErrorResponse errorResponse = new ErrorResponse("IMPORT_INIT_FAILED", errorMessage);
            return Response.serverError().entity(errorResponse).build();
        }
    }

    /**
     * Retrieves import results for a completed import operation.
     * Returns 202 Accepted if import is still in progress.
     */
    @Path("/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportResults(@PathParam("processId") String processId) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            ErrorResponse errorResponse = new ErrorResponse("PROCESS_NOT_FOUND", "Process not found");
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        // If process is still running, return 202 Accepted
        if (processStatus.getStatus() == ProcessStatus.Status.RUNNING) {
            ProcessProgressResponse progressResponse = new ProcessProgressResponse("running", processStatus.getProgress(),
                    "Import in progress");
            return Response.accepted().entity(progressResponse).build();
        }

        // If process failed, return error
        if (processStatus.getStatus() == ProcessStatus.Status.FAILED) {
            String errorMessage = "Import operation failed. Please check the process logs for details.";
            ErrorResponse errorResponse = new ErrorResponse("failed", "IMPORT_FAILED", errorMessage);
            return Response.serverError().entity(errorResponse).build();
        }

        // If process was cancelled, return error
        if (processStatus.getStatus() == ProcessStatus.Status.CANCELLED) {
            ErrorResponse errorResponse = new ErrorResponse("cancelled", "IMPORT_CANCELLED", "Import was cancelled");
            return Response.status(Response.Status.GONE).entity(errorResponse).build();
        }

        // Import completed successfully - return results
        ImportResultsResponse resultsResponse = new ImportResultsResponse("completed", processId, 100.0,
                processStatus.getCompletionTimeMillis(), "Import completed successfully");
        return Response.ok().entity(resultsResponse).build();
    }

    /**
     * Cancels an in-progress import operation.
     */
    @Path("/{processId}")
    @DELETE
    public Response cancelImport(@PathParam("processId") String processId) {

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
            String docbasePropNamesParam) throws IOException {

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
                dataUrlSizeThresholdParam, docbasePropNamesParam, null, null);

        return params;
    }

    /**
     * Writes uploaded package content to import file.
     */
    private void writePackageToFile(Attachment packageAttachment, String importFilePath) throws IOException {
        try (InputStream in = packageAttachment.getObject(InputStream.class);
                FileOutputStream out = new FileOutputStream(new File(importFilePath))) {
            IOUtils.copy(in, out);
        }
    }

}
