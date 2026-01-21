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
import java.io.PrintStream;

import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ZipCompressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for executing export asynchronously.
 */
public class AsyncExportTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AsyncExportTask.class);

    private final ProcessStatus processStatus;
    private final String exportFilePath;
    private final ExecutionParams params;
    private final ProcessFileManager fileManager;
    private final ContentEximExportService exportService;
    private final ProcessMonitor processMonitor;

    public AsyncExportTask(ProcessStatus processStatus, String exportFilePath, ExecutionParams params,
            ProcessFileManager fileManager, ContentEximExportService exportService, ProcessMonitor processMonitor) {
        this.processStatus = processStatus;
        this.exportFilePath = exportFilePath;
        this.params = params;
        this.fileManager = fileManager;
        this.exportService = exportService;
        this.processMonitor = processMonitor;
    }

    @Override
    public void run() {
        Logger procLogger = log;
        File tempLogFile = null;
        PrintStream tempLogOut = null;

        try {
            // Create temporary log file
            tempLogFile = File.createTempFile("exim-export-async", ".log");
            tempLogOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(tempLogFile)));
            procLogger = new TeeLogger(log, tempLogOut);

            procLogger.info("AsyncExportTask started for process {}", processStatus.getId());

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

            processMonitor.stopProcess(processStatus);
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

    /**
     * Simple logger wrapper that tees output to both a Logger and a PrintStream.
     */
    private static class TeeLogger implements Logger {

        private final Logger baseLogger;
        private final PrintStream out;

        TeeLogger(Logger baseLogger, PrintStream out) {
            this.baseLogger = baseLogger;
            this.out = out;
        }

        @Override
        public String getName() {
            return baseLogger.getName();
        }

        @Override
        public boolean isTraceEnabled() {
            return baseLogger.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            baseLogger.trace(msg);
            out.println("[TRACE] " + msg);
            out.flush();
        }

        @Override
        public void trace(String format, Object arg) {
            baseLogger.trace(format, arg);
            String msg = String.format(format, arg);
            out.println("[TRACE] " + msg);
            out.flush();
        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {
            baseLogger.trace(format, arg1, arg2);
            String msg = String.format(format, arg1, arg2);
            out.println("[TRACE] " + msg);
            out.flush();
        }

        @Override
        public void trace(String format, Object... arguments) {
            baseLogger.trace(format, arguments);
            String msg = String.format(format, arguments);
            out.println("[TRACE] " + msg);
            out.flush();
        }

        @Override
        public void trace(String msg, Throwable t) {
            baseLogger.trace(msg, t);
            out.println("[TRACE] " + msg);
            t.printStackTrace(out);
            out.flush();
        }

        @Override
        public boolean isDebugEnabled() {
            return baseLogger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            baseLogger.debug(msg);
            out.println("[DEBUG] " + msg);
            out.flush();
        }

        @Override
        public void debug(String format, Object arg) {
            baseLogger.debug(format, arg);
            String msg = String.format(format, arg);
            out.println("[DEBUG] " + msg);
            out.flush();
        }

        @Override
        public void debug(String format, Object arg1, Object arg2) {
            baseLogger.debug(format, arg1, arg2);
            String msg = String.format(format, arg1, arg2);
            out.println("[DEBUG] " + msg);
            out.flush();
        }

        @Override
        public void debug(String format, Object... arguments) {
            baseLogger.debug(format, arguments);
            String msg = String.format(format, arguments);
            out.println("[DEBUG] " + msg);
            out.flush();
        }

        @Override
        public void debug(String msg, Throwable t) {
            baseLogger.debug(msg, t);
            out.println("[DEBUG] " + msg);
            t.printStackTrace(out);
            out.flush();
        }

        @Override
        public boolean isInfoEnabled() {
            return baseLogger.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            baseLogger.info(msg);
            out.println("[INFO] " + msg);
            out.flush();
        }

        @Override
        public void info(String format, Object arg) {
            baseLogger.info(format, arg);
            String msg = String.format(format, arg);
            out.println("[INFO] " + msg);
            out.flush();
        }

        @Override
        public void info(String format, Object arg1, Object arg2) {
            baseLogger.info(format, arg1, arg2);
            String msg = String.format(format, arg1, arg2);
            out.println("[INFO] " + msg);
            out.flush();
        }

        @Override
        public void info(String format, Object... arguments) {
            baseLogger.info(format, arguments);
            String msg = String.format(format, arguments);
            out.println("[INFO] " + msg);
            out.flush();
        }

        @Override
        public void info(String msg, Throwable t) {
            baseLogger.info(msg, t);
            out.println("[INFO] " + msg);
            t.printStackTrace(out);
            out.flush();
        }

        @Override
        public boolean isWarnEnabled() {
            return baseLogger.isWarnEnabled();
        }

        @Override
        public void warn(String msg) {
            baseLogger.warn(msg);
            out.println("[WARN] " + msg);
            out.flush();
        }

        @Override
        public void warn(String format, Object arg) {
            baseLogger.warn(format, arg);
            String msg = String.format(format, arg);
            out.println("[WARN] " + msg);
            out.flush();
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
            baseLogger.warn(format, arg1, arg2);
            String msg = String.format(format, arg1, arg2);
            out.println("[WARN] " + msg);
            out.flush();
        }

        @Override
        public void warn(String format, Object... arguments) {
            baseLogger.warn(format, arguments);
            String msg = String.format(format, arguments);
            out.println("[WARN] " + msg);
            out.flush();
        }

        @Override
        public void warn(String msg, Throwable t) {
            baseLogger.warn(msg, t);
            out.println("[WARN] " + msg);
            t.printStackTrace(out);
            out.flush();
        }

        @Override
        public boolean isErrorEnabled() {
            return baseLogger.isErrorEnabled();
        }

        @Override
        public void error(String msg) {
            baseLogger.error(msg);
            out.println("[ERROR] " + msg);
            out.flush();
        }

        @Override
        public void error(String format, Object arg) {
            baseLogger.error(format, arg);
            String msg = String.format(format, arg);
            out.println("[ERROR] " + msg);
            out.flush();
        }

        @Override
        public void error(String format, Object arg1, Object arg2) {
            baseLogger.error(format, arg1, arg2);
            String msg = String.format(format, arg1, arg2);
            out.println("[ERROR] " + msg);
            out.flush();
        }

        @Override
        public void error(String format, Object... arguments) {
            baseLogger.error(format, arguments);
            String msg = String.format(format, arguments);
            out.println("[ERROR] " + msg);
            out.flush();
        }

        @Override
        public void error(String msg, Throwable t) {
            baseLogger.error(msg, t);
            out.println("[ERROR] " + msg);
            t.printStackTrace(out);
            out.flush();
        }

        // Marker-based logging methods (SLF4J 2.0+)
        // These methods delegate to the base logger without additional output
        @Override
        public boolean isTraceEnabled(org.slf4j.Marker marker) {
            return baseLogger.isTraceEnabled(marker);
        }

        @Override
        public void trace(org.slf4j.Marker marker, String msg) {
            trace(msg);
        }

        @Override
        public void trace(org.slf4j.Marker marker, String format, Object arg) {
            trace(format, arg);
        }

        @Override
        public void trace(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {
            trace(format, arg1, arg2);
        }

        @Override
        public void trace(org.slf4j.Marker marker, String format, Object... arguments) {
            trace(format, arguments);
        }

        @Override
        public void trace(org.slf4j.Marker marker, String msg, Throwable t) {
            trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled(org.slf4j.Marker marker) {
            return baseLogger.isDebugEnabled(marker);
        }

        @Override
        public void debug(org.slf4j.Marker marker, String msg) {
            debug(msg);
        }

        @Override
        public void debug(org.slf4j.Marker marker, String format, Object arg) {
            debug(format, arg);
        }

        @Override
        public void debug(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {
            debug(format, arg1, arg2);
        }

        @Override
        public void debug(org.slf4j.Marker marker, String format, Object... arguments) {
            debug(format, arguments);
        }

        @Override
        public void debug(org.slf4j.Marker marker, String msg, Throwable t) {
            debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled(org.slf4j.Marker marker) {
            return baseLogger.isInfoEnabled(marker);
        }

        @Override
        public void info(org.slf4j.Marker marker, String msg) {
            info(msg);
        }

        @Override
        public void info(org.slf4j.Marker marker, String format, Object arg) {
            info(format, arg);
        }

        @Override
        public void info(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {
            info(format, arg1, arg2);
        }

        @Override
        public void info(org.slf4j.Marker marker, String format, Object... arguments) {
            info(format, arguments);
        }

        @Override
        public void info(org.slf4j.Marker marker, String msg, Throwable t) {
            info(msg, t);
        }

        @Override
        public boolean isWarnEnabled(org.slf4j.Marker marker) {
            return baseLogger.isWarnEnabled(marker);
        }

        @Override
        public void warn(org.slf4j.Marker marker, String msg) {
            warn(msg);
        }

        @Override
        public void warn(org.slf4j.Marker marker, String format, Object arg) {
            warn(format, arg);
        }

        @Override
        public void warn(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {
            warn(format, arg1, arg2);
        }

        @Override
        public void warn(org.slf4j.Marker marker, String format, Object... arguments) {
            warn(format, arguments);
        }

        @Override
        public void warn(org.slf4j.Marker marker, String msg, Throwable t) {
            warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled(org.slf4j.Marker marker) {
            return baseLogger.isErrorEnabled(marker);
        }

        @Override
        public void error(org.slf4j.Marker marker, String msg) {
            error(msg);
        }

        @Override
        public void error(org.slf4j.Marker marker, String format, Object arg) {
            error(format, arg);
        }

        @Override
        public void error(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {
            error(format, arg1, arg2);
        }

        @Override
        public void error(org.slf4j.Marker marker, String format, Object... arguments) {
            error(format, arguments);
        }

        @Override
        public void error(org.slf4j.Marker marker, String msg, Throwable t) {
            error(msg, t);
        }
    }
}
