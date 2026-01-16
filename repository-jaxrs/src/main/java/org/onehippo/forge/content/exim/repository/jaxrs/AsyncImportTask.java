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

import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for executing import asynchronously.
 */
public class AsyncImportTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AsyncImportTask.class);

    private final ProcessStatus processStatus;
    private final String importFilePath;
    private final ExecutionParams params;
    private final ProcessFileManager fileManager;
    private final ContentEximImportService importService;
    private final ProcessMonitor processMonitor;

    public AsyncImportTask(ProcessStatus processStatus, String importFilePath, ExecutionParams params,
            ProcessFileManager fileManager, ContentEximImportService importService, ProcessMonitor processMonitor) {
        this.processStatus = processStatus;
        this.importFilePath = importFilePath;
        this.params = params;
        this.fileManager = fileManager;
        this.importService = importService;
        this.processMonitor = processMonitor;
    }

    @Override
    public void run() {
        Logger procLogger = log;

        try {
            procLogger.info("AsyncImportTask started for process {}", processStatus.getId());

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

            // Cleanup import file after processing
            try {
                fileManager.deleteImportFile(importFilePath);
            } catch (Exception e) {
                procLogger.warn("Failed to cleanup import file after processing", e);
            }

            processMonitor.stopProcess(processStatus);
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
