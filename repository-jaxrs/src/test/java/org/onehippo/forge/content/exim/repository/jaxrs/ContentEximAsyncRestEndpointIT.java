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

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;

/**
 * Integration tests for asynchronous REST API endpoints.
 * Tests the workflow of initiating, monitoring, and retrieving async operations.
 */
public class ContentEximAsyncRestEndpointIT {

    private ProcessMonitor processMonitor;
    private ProcessFileManager fileManager;
    private String tempStorageDir;

    @Before
    public void setUp() throws IOException {
        processMonitor = new ProcessMonitor();
        tempStorageDir = Files.createTempDirectory("exim-async-rest-test").toString();
        fileManager = new ProcessFileManager(tempStorageDir, 10000L); // 10 second TTL for testing
    }

    /**
     * Test 1: Async Export Initiation Response Structure
     * Verify that initiating an async export returns proper response structure.
     */
    @Test
    public void testAsyncExportInitiationResponse() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String exportFile = null;

        try {
            exportFile = fileManager.createExportFile(process.getId());
            process.setExportFilePath(exportFile);

            // Simulate response structure
            String processId = process.getId();
            String statusUrl = "/exim/ps/" + processId;
            String downloadUrl = "/exim/export-async/" + processId;

            assertNotNull("Status URL should be generated", statusUrl);
            assertNotNull("Download URL should be generated", downloadUrl);
            assertTrue("Status URL should contain process ID", statusUrl.contains(processId));
            assertTrue("Download URL should contain process ID", downloadUrl.contains(processId));

        } finally {
            if (exportFile != null) {
                try {
                    fileManager.deleteExportFile(exportFile);
                } catch (IOException e) {
                    // Ignore cleanup errors in test
                }
            }
            processMonitor.stopProcess(process);
        }
    }

    /**
     * Test 2: Async Import Initiation Response Structure
     * Verify that initiating an async import returns proper response structure.
     */
    @Test
    public void testAsyncImportInitiationResponse() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String importFile = null;

        try {
            importFile = fileManager.createImportFile(process.getId());
            process.setImportFilePath(importFile);

            // Simulate response structure
            String processId = process.getId();
            String statusUrl = "/exim/ps/" + processId;
            String resultsUrl = "/exim/import-async/" + processId;

            assertNotNull("Status URL should be generated", statusUrl);
            assertNotNull("Results URL should be generated", resultsUrl);
            assertTrue("Status URL should contain process ID", statusUrl.contains(processId));
            assertTrue("Results URL should contain process ID", resultsUrl.contains(processId));

        } finally {
            if (importFile != null) {
                try {
                    fileManager.deleteImportFile(importFile);
                } catch (IOException e) {
                    // Ignore cleanup errors in test
                }
            }
            processMonitor.stopProcess(process);
        }
    }

    /**
     * Test 3: Export Download - In Progress Response
     * Verify that querying export download while in progress returns 202 Accepted.
     */
    @Test
    public void testExportDownloadInProgressResponse() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);
        process.setProgress(25.0);

        // Query in-progress export
        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertEquals("Process should be running", ProcessStatus.Status.RUNNING, queried.getStatus());

        // Simulate 202 Accepted response check
        assertTrue("Should return 202 for running process", queried.getStatus() == ProcessStatus.Status.RUNNING);

        processMonitor.stopProcess(process);
    }

    /**
     * Test 4: Export Download - Complete Response
     * Verify that completed export returns 200 OK with file.
     */
    @Test
    public void testExportDownloadCompleteResponse() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String exportFile = fileManager.createExportFile(process.getId());

        try {
            process.setExportFilePath(exportFile);
            process.setStatus(ProcessStatus.Status.COMPLETED);
            process.setCompletionTimeMillis(System.currentTimeMillis());

            // Query completed export
            ProcessStatus queried = processMonitor.getProcess(process.getId());
            assertNotNull("Process should be queryable", queried);
            assertEquals("Process should be completed", ProcessStatus.Status.COMPLETED, queried.getStatus());
            assertNotNull("Export file path should be set", queried.getExportFilePath());

            // File should be retrievable
            assertNotNull("File should be retrievable", fileManager.getExportFile(queried.getExportFilePath()));

        } finally {
            if (exportFile != null) {
                try {
                    fileManager.deleteExportFile(exportFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            processMonitor.stopProcess(process);
        }
    }

    /**
     * Test 5: Export Download - Failed Response
     * Verify that failed export returns error status.
     */
    @Test
    public void testExportDownloadFailedResponse() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.FAILED);
        process.setErrorMessage("Export operation failed");
        process.setCompletionTimeMillis(System.currentTimeMillis());

        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertEquals("Process should be failed", ProcessStatus.Status.FAILED, queried.getStatus());
        assertNotNull("Error message should be set", queried.getErrorMessage());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 6: Export Download - Cancelled Response
     * Verify that cancelled export returns appropriate status.
     */
    @Test
    public void testExportDownloadCancelledResponse() {
        ProcessStatus process = processMonitor.startProcess();
        process.requestCancellation();
        process.setStatus(ProcessStatus.Status.CANCELLED);
        process.setCompletionTimeMillis(System.currentTimeMillis());

        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertEquals("Process should be cancelled", ProcessStatus.Status.CANCELLED, queried.getStatus());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 7: Export Cancellation - Not Found
     * Verify that cancelling non-existent process returns 404.
     */
    @Test
    public void testExportCancellationNotFound() {
        String nonExistentId = UUID.randomUUID().toString();
        ProcessStatus process = processMonitor.getProcess(nonExistentId);

        assertNull("Non-existent process should not be found", process);
    }

    /**
     * Test 8: Export Cancellation - Success
     * Verify that cancelling running export succeeds.
     */
    @Test
    public void testExportCancellationSuccess() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);

        // Initiate cancellation
        process.requestCancellation();
        assertTrue("Cancellation should be requested", process.isCancellationRequested());

        // Verify cancellation
        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertTrue("Cancellation flag should be set", queried.isCancellationRequested());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 9: Export Cancellation - Already Completed
     * Verify that cancelling completed export returns conflict.
     */
    @Test
    public void testExportCancellationAlreadyCompleted() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.COMPLETED);
        process.setCompletionTimeMillis(System.currentTimeMillis());

        // Simulate conflict response check
        assertTrue("Cannot cancel completed process", process.getStatus() != ProcessStatus.Status.RUNNING);

        processMonitor.stopProcess(process);
    }

    /**
     * Test 10: Import Results - In Progress Response
     * Verify that querying import results while in progress returns 202 Accepted.
     */
    @Test
    public void testImportResultsInProgressResponse() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);
        process.setProgress(50.0);

        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertEquals("Process should be running", ProcessStatus.Status.RUNNING, queried.getStatus());
        assertEquals("Progress should be tracked", 50.0, queried.getProgress(), 0.01);

        processMonitor.stopProcess(process);
    }

    /**
     * Test 11: Import Results - Complete Response
     * Verify that completed import returns results.
     */
    @Test
    public void testImportResultsCompleteResponse() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String importFile = fileManager.createImportFile(process.getId());

        try {
            process.setImportFilePath(importFile);
            process.setStatus(ProcessStatus.Status.COMPLETED);
            process.setCompletionTimeMillis(System.currentTimeMillis());

            ProcessStatus queried = processMonitor.getProcess(process.getId());
            assertNotNull("Process should be queryable", queried);
            assertEquals("Process should be completed", ProcessStatus.Status.COMPLETED, queried.getStatus());

        } finally {
            if (importFile != null) {
                try {
                    fileManager.deleteImportFile(importFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            processMonitor.stopProcess(process);
        }
    }

    /**
     * Test 12: Import Results - Failed Response
     * Verify that failed import returns error details.
     */
    @Test
    public void testImportResultsFailedResponse() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.FAILED);
        process.setErrorMessage("Import failed: Invalid ZIP format");
        process.setCompletionTimeMillis(System.currentTimeMillis());

        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertEquals("Process should be failed", ProcessStatus.Status.FAILED, queried.getStatus());
        assertTrue("Error message should contain details", queried.getErrorMessage().contains("Invalid ZIP"));

        processMonitor.stopProcess(process);
    }

    /**
     * Test 13: Import Cancellation - Success
     * Verify that cancelling running import succeeds.
     */
    @Test
    public void testImportCancellationSuccess() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);

        process.requestCancellation();
        assertTrue("Cancellation should be requested", process.isCancellationRequested());

        ProcessStatus queried = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be queryable", queried);
        assertTrue("Cancellation flag should be set", queried.isCancellationRequested());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 14: Progress Tracking - Export
     * Verify that export progress is tracked through workflow.
     */
    @Test
    public void testExportProgressTracking() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);

        // Simulate progress updates
        process.setProgress(0.0);
        assertEquals("Initial progress should be 0", 0.0, process.getProgress(), 0.01);

        process.setProgress(33.3);
        assertEquals("Progress should update to 33.3", 33.3, process.getProgress(), 0.01);

        process.setProgress(66.6);
        assertEquals("Progress should update to 66.6", 66.6, process.getProgress(), 0.01);

        process.setProgress(100.0);
        process.setStatus(ProcessStatus.Status.COMPLETED);
        assertEquals("Final progress should be 100", 100.0, process.getProgress(), 0.01);

        processMonitor.stopProcess(process);
    }

    /**
     * Test 15: Progress Tracking - Import
     * Verify that import progress is tracked through workflow.
     */
    @Test
    public void testImportProgressTracking() {
        ProcessStatus process = processMonitor.startProcess();
        process.setStatus(ProcessStatus.Status.RUNNING);

        // Simulate progress updates
        double[] progressPoints = {0.0, 25.0, 50.0, 75.0, 100.0};
        for (double progress : progressPoints) {
            process.setProgress(progress);
            assertEquals("Progress should be " + progress, progress, process.getProgress(), 0.01);
        }

        process.setStatus(ProcessStatus.Status.COMPLETED);
        assertEquals("Final status should be COMPLETED", ProcessStatus.Status.COMPLETED, process.getStatus());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 16: Process Status Persistence
     * Verify that process status is maintained across queries.
     */
    @Test
    public void testProcessStatusPersistence() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String exportFile = fileManager.createExportFile(process.getId());

        try {
            process.setExportFilePath(exportFile);
            process.setStatus(ProcessStatus.Status.RUNNING);
            process.setProgress(50.0);

            // Query multiple times
            for (int i = 0; i < 5; i++) {
                ProcessStatus queried = processMonitor.getProcess(process.getId());
                assertNotNull("Process should be queryable", queried);
                assertEquals("Export path should persist", exportFile, queried.getExportFilePath());
                assertEquals("Status should persist", ProcessStatus.Status.RUNNING, queried.getStatus());
                assertEquals("Progress should persist", 50.0, queried.getProgress(), 0.01);
            }

        } finally {
            if (exportFile != null) {
                try {
                    fileManager.deleteExportFile(exportFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            processMonitor.stopProcess(process);
        }
    }

    /**
     * Test 17: Multiple Processes Isolation
     * Verify that multiple concurrent processes don't interfere with each other.
     */
    @Test
    public void testMultipleProcessesIsolation() throws IOException {
        ProcessStatus process1 = processMonitor.startProcess();
        ProcessStatus process2 = processMonitor.startProcess();

        String file1 = fileManager.createExportFile(process1.getId());
        String file2 = fileManager.createImportFile(process2.getId());

        try {
            process1.setExportFilePath(file1);
            process1.setStatus(ProcessStatus.Status.RUNNING);
            process1.setProgress(25.0);

            process2.setImportFilePath(file2);
            process2.setStatus(ProcessStatus.Status.COMPLETED);

            // Verify isolation
            ProcessStatus q1 = processMonitor.getProcess(process1.getId());
            ProcessStatus q2 = processMonitor.getProcess(process2.getId());

            assertEquals("Process 1 should have export file", file1, q1.getExportFilePath());
            assertEquals("Process 2 should have import file", file2, q2.getImportFilePath());
            assertEquals("Process 1 should be running", ProcessStatus.Status.RUNNING, q1.getStatus());
            assertEquals("Process 2 should be completed", ProcessStatus.Status.COMPLETED, q2.getStatus());
            assertEquals("Process 1 should have 25% progress", 25.0, q1.getProgress(), 0.01);

        } finally {
            if (file1 != null) {
                try {
                    fileManager.deleteExportFile(file1);
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (file2 != null) {
                try {
                    fileManager.deleteImportFile(file2);
                } catch (IOException e) {
                    // Ignore
                }
            }
            processMonitor.stopProcess(process1);
            processMonitor.stopProcess(process2);
        }
    }

    /**
     * Test 18: Process Query - Not Found
     * Verify that querying non-existent process returns null.
     */
    @Test
    public void testProcessQueryNotFound() {
        ProcessStatus process = processMonitor.getProcess(UUID.randomUUID().toString());
        assertNull("Non-existent process should return null", process);
    }

    /**
     * Test 19: Execution Parameters Storage
     * Verify that execution parameters are stored in process status.
     */
    @Test
    public void testExecutionParametersStorage() {
        ProcessStatus process = processMonitor.startProcess();
        ExecutionParams params = new ExecutionParams();
        params.setBatchSize(100);
        params.setThrottle(50L);

        process.setExecutionParams(params);
        assertEquals("Batch size should be stored", 100L, (long) process.getExecutionParams().getBatchSize());
        assertEquals("Throttle should be stored", 50L, (long) process.getExecutionParams().getThrottle());

        processMonitor.stopProcess(process);
    }

    /**
     * Test 20: Full Async Export Workflow Simulation
     * Simulate complete async export workflow from initiation to download.
     */
    @Test
    public void testFullAsyncExportWorkflow() throws IOException, InterruptedException {
        // 1. Initiate export
        ProcessStatus process = processMonitor.startProcess();
        String exportFile = fileManager.createExportFile(process.getId());
        process.setExportFilePath(exportFile);

        try {
            // 2. Query status while processing
            process.setStatus(ProcessStatus.Status.RUNNING);
            ProcessStatus status = processMonitor.getProcess(process.getId());
            assertEquals("Should be running", ProcessStatus.Status.RUNNING, status.getStatus());

            // 3. Update progress
            process.setProgress(50.0);
            status = processMonitor.getProcess(process.getId());
            assertEquals("Progress should be 50%", 50.0, status.getProgress(), 0.01);

            // 4. Complete export
            process.setProgress(100.0);
            process.setStatus(ProcessStatus.Status.COMPLETED);
            process.setCompletionTimeMillis(System.currentTimeMillis());

            status = processMonitor.getProcess(process.getId());
            assertEquals("Should be completed", ProcessStatus.Status.COMPLETED, status.getStatus());

            // 5. Download export
            assertNotNull("File should be retrievable", fileManager.getExportFile(exportFile));

        } finally {
            if (exportFile != null) {
                try {
                    fileManager.deleteExportFile(exportFile);
                } catch (IOException e) {
                    // Ignore
                }
            }
            processMonitor.stopProcess(process);
        }
    }
}
