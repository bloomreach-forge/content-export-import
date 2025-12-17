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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
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
                return Response.status(Response.Status.BAD_REQUEST).entity("Package attachment is required").build();
            }

            // Start process and create import file
            processStatus = getProcessMonitor().startProcess();
            fillProcessStatusByRequestInfo(processStatus, securityContext, request);
            importFilePath = fileManager.createImportFile(processStatus.getId());
            processStatus.setImportFilePath(importFilePath);

            // Write uploaded package to import file
            writePackageToFile(packageAttachment, importFilePath);

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
                    dataUrlSizeThresholdParam, docbasePropNamesParam, null, null);
            processStatus.setExecutionParams(params);

            // Submit import task to executor
            AsyncImportTask importTask = new AsyncImportTask(processStatus, importFilePath, params);
            executorService.submit(importTask);

            // Return response with process ID
            String responseJson = String.format(
                    "{\"processId\": %d, \"statusUrl\": \"/exim/ps/%d\", \"resultsUrl\": \"/exim/import-async/%d\"}",
                    processStatus.getId(), processStatus.getId(), processStatus.getId());

            return Response.accepted().entity(responseJson).build();

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
            return Response.serverError().entity(String.format("{\"error\": \"%s\"}", e.getMessage())).build();
        }
    }

    /**
     * Retrieves import results for a completed import operation.
     * Returns 202 Accepted if import is still in progress.
     */
    @Path("/{processId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportResults(@PathParam("processId") long processId) {

        ProcessStatus processStatus = getProcessMonitor().getProcess(processId);

        if (processStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Process not found\"}").build();
        }

        // If process is still running, return 202 Accepted
        if (processStatus.getStatus() == ProcessStatus.Status.RUNNING) {
            String responseJson = String.format(
                    "{\"status\": \"running\", \"progress\": %.2f, \"message\": \"Import in progress\"}",
                    processStatus.getProgress());
            return Response.accepted().entity(responseJson).build();
        }

        // If process failed, return error
        if (processStatus.getStatus() == ProcessStatus.Status.FAILED) {
            String errorJson = String.format("{\"status\": \"failed\", \"error\": \"%s\"}", processStatus.getErrorMessage());
            return Response.serverError().entity(errorJson).build();
        }

        // If process was cancelled, return error
        if (processStatus.getStatus() == ProcessStatus.Status.CANCELLED) {
            return Response.status(Response.Status.GONE).entity("{\"status\": \"cancelled\"}").build();
        }

        // Import completed successfully - return results
        String resultJson = String.format(
                "{\"status\": \"completed\", \"processId\": %d, \"progress\": 100.0, \"completionTime\": %d, \"message\": \"Import completed successfully\"}",
                processId, processStatus.getCompletionTimeMillis());
        return Response.ok().entity(resultJson).build();
    }

    /**
     * Cancels an in-progress import operation.
     */
    @Path("/{processId}")
    @DELETE
    public Response cancelImport(@PathParam("processId") long processId) {

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
     * Writes uploaded package content to import file.
     */
    private void writePackageToFile(Attachment packageAttachment, String importFilePath) throws IOException {
        try (InputStream in = packageAttachment.getObject(InputStream.class);
                FileOutputStream out = new FileOutputStream(new File(importFilePath))) {
            IOUtils.copy(in, out);
        }
    }

    /**
     * Task for executing import asynchronously.
     */
    private class AsyncImportTask implements Runnable {

        private final ProcessStatus processStatus;
        private final String importFilePath;
        private final ExecutionParams params;

        AsyncImportTask(ProcessStatus processStatus, String importFilePath, ExecutionParams params) {
            this.processStatus = processStatus;
            this.importFilePath = importFilePath;
            this.params = params;
        }

        @Override
        public void run() {
            Logger procLogger = log;
            PrintStream logOut = null;

            try {
                procLogger.info("AsyncImportTask started for process {}", processStatus.getId());

                // Call the synchronous import service to do the actual work
                // This reuses the existing import logic
                // For now, we'll document that this should delegate to the import service
                performImport(procLogger);

                processStatus.setStatus(ProcessStatus.Status.COMPLETED);
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());
                procLogger.info("AsyncImportTask completed successfully for process {}", processStatus.getId());

            } catch (Exception e) {
                procLogger.error("AsyncImportTask failed for process {}", processStatus.getId(), e);
                processStatus.setStatus(ProcessStatus.Status.FAILED);
                processStatus.setErrorMessage(e.getMessage());
                processStatus.setCompletionTimeMillis(System.currentTimeMillis());

            } finally {
                procLogger.info("AsyncImportTask finally block for process {}", processStatus.getId());

                if (logOut != null) {
                    IOUtils.closeQuietly(logOut);
                }

                // Cleanup import file after processing
                try {
                    fileManager.deleteImportFile(importFilePath);
                } catch (IOException e) {
                    procLogger.warn("Failed to cleanup import file after processing", e);
                }

                getProcessMonitor().stopProcess(processStatus);
            }
        }

        private void performImport(Logger procLogger) throws Exception {
            File importFile = new File(importFilePath);
            FileObject baseFolder = null;
            Session session = null;

            try {
                // Open the ZIP file as a VFS FileObject
                baseFolder = VFS.getManager().resolveFile("zip:" + importFile.toURI());
                procLogger.info("Starting async import from ZIP: {}", importFile.getAbsolutePath());

                // Create JCR session
                session = importService.createSession();

                // Perform core import
                Result result = importService.performImportCore(procLogger, processStatus, baseFolder, session, params);

                session.logout();
                session = null;

                procLogger.info("Core import completed successfully");
                procLogger.info("Import statistics - Documents: {}/{}, Binaries: {}/{}",
                        result.getSucceededDocumentCount(), result.getTotalDocumentCount(),
                        result.getSucceededBinaryCount(), result.getTotalBinaryCount());

            } finally {
                if (baseFolder != null) {
                    try {
                        baseFolder.close();
                    } catch (Exception e) {
                        procLogger.error("Failed to close VFS zip file folder lock", e);
                    }
                }
                if (session != null) {
                    try {
                        session.logout();
                    } catch (Exception e) {
                        procLogger.error("Failed to logout session", e);
                    }
                }
            }
        }
    }
}
