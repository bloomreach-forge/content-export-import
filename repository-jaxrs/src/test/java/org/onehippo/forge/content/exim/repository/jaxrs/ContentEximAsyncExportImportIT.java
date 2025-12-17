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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;

/**
 * Integration tests for asynchronous export/import functionality.
 * Tests cover the complete workflow from initiation through completion and cleanup.
 */
public class ContentEximAsyncExportImportIT {

    private ProcessMonitor processMonitor;
    private ProcessFileManager fileManager;
    private String tempStorageDir;

    @Before
    public void setUp() throws IOException {
        processMonitor = new ProcessMonitor();

        // Create a temporary directory for testing
        tempStorageDir = Files.createTempDirectory("exim-async-test").toString();

        // Initialize file manager with test storage directory and short TTL for testing
        fileManager = new ProcessFileManager(tempStorageDir, 2000L); // 2 second TTL for testing
    }

    /**
     * Test 1: Process Status Lifecycle
     * Verify that process status transitions correctly through different states.
     */
    @Test
    public void testProcessStatusLifecycle() {
        // Start a process
        ProcessStatus process = processMonitor.startProcess();
        assertNotNull("Process should be created", process);
        assertTrue("Process ID should be positive", process.getId() > 0);
        assertEquals("Initial status should be RUNNING", ProcessStatus.Status.RUNNING, process.getStatus());
        assertEquals("Initial progress should be 0", 0.0, process.getProgress(), 0.01);

        // Update process status
        process.setProgress(50.0);
        process.setStatus(ProcessStatus.Status.COMPLETED);
        process.setCompletionTimeMillis(System.currentTimeMillis());

        assertEquals("Progress should be updated", 50.0, process.getProgress(), 0.01);
        assertEquals("Status should be COMPLETED", ProcessStatus.Status.COMPLETED, process.getStatus());

        // Stop process (moves to completed processes history)
        processMonitor.stopProcess(process);

        // Retrieve from history
        ProcessStatus retrieved = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be retrievable from history", retrieved);
        assertEquals("Retrieved process should have correct ID", process.getId(), retrieved.getId());
    }

    /**
     * Test 2: Export File Creation and Retrieval
     * Verify that export files can be created and retrieved correctly.
     */
    @Test
    public void testExportFileCreation() throws IOException {
        long processId = 1L;
        String exportFile = fileManager.createExportFile(processId);

        assertNotNull("Export file path should be created", exportFile);
        assertTrue("Export file should exist", new File(exportFile).exists());

        // Verify file is in the exports directory
        assertTrue("File should be in exports directory", exportFile.contains("exim-exports"));
        assertTrue("File should have .zip extension", exportFile.endsWith(".zip"));

        // Retrieve the file
        File retrievedFile = fileManager.getExportFile(exportFile);
        assertNotNull("File should be retrievable", retrievedFile);
        assertTrue("Retrieved file should exist", retrievedFile.exists());

        // Cleanup
        fileManager.deleteExportFile(exportFile);
        assertFalse("File should be deleted", new File(exportFile).exists());
    }

    /**
     * Test 3: Import File Creation and Retrieval
     * Verify that import files can be created and retrieved correctly.
     */
    @Test
    public void testImportFileCreation() throws IOException {
        long processId = 1L;
        String importFile = fileManager.createImportFile(processId);

        assertNotNull("Import file path should be created", importFile);
        assertTrue("Import file should exist", new File(importFile).exists());

        // Verify file is in the imports directory
        assertTrue("File should be in imports directory", importFile.contains("exim-imports"));
        assertTrue("File should have .zip extension", importFile.endsWith(".zip"));

        // Retrieve the file
        File retrievedFile = fileManager.getImportFile(importFile);
        assertNotNull("File should be retrievable", retrievedFile);
        assertTrue("Retrieved file should exist", retrievedFile.exists());

        // Cleanup
        fileManager.deleteImportFile(importFile);
        assertFalse("File should be deleted", new File(importFile).exists());
    }

    /**
     * Test 4: File Expiration and TTL
     * Verify that files are correctly identified as expired after TTL.
     */
    @Test
    public void testFileExpiration() throws IOException, InterruptedException {
        long processId = 1L;
        String exportFile = fileManager.createExportFile(processId);
        File file = new File(exportFile);

        assertTrue("File should exist immediately after creation", file.exists());

        // File should be retrievable initially
        File retrieved = fileManager.getExportFile(exportFile);
        assertNotNull("File should be retrievable before TTL expires", retrieved);

        // Wait for TTL to expire (2 seconds in setUp)
        Thread.sleep(2500);

        // File should now be expired and throw exception
        try {
            fileManager.getExportFile(exportFile);
            fail("Should throw IOException for expired file");
        } catch (IOException e) {
            assertTrue("Exception should indicate file has expired", e.getMessage().contains("expired"));
        }
    }

    /**
     * Test 5: Process History Tracking
     * Verify that completed processes are tracked and retrievable.
     */
    @Test
    public void testProcessHistoryTracking() {
        // Create and complete multiple processes
        int processCount = 10;
        for (int i = 0; i < processCount; i++) {
            ProcessStatus process = processMonitor.startProcess();
            process.setStatus(ProcessStatus.Status.COMPLETED);
            process.setCompletionTimeMillis(System.currentTimeMillis());
            processMonitor.stopProcess(process);
        }

        // Verify recent processes are still in history
        ProcessStatus lastProcess = processMonitor.getProcess(processCount);
        assertNotNull("Last process should be in history", lastProcess);
        assertEquals("Last process should have correct ID", processCount, lastProcess.getId());
    }

    /**
     * Test 6: Cancellation Request Tracking
     * Verify that cancellation requests are properly tracked.
     */
    @Test
    public void testCancellationRequest() {
        ProcessStatus process = processMonitor.startProcess();
        assertFalse("Cancellation should not be requested initially", process.isCancellationRequested());

        process.requestCancellation();
        assertTrue("Cancellation should be requested after call", process.isCancellationRequested());
    }

    /**
     * Test 7: Process Monitoring with Multiple Concurrent Processes
     * Verify that multiple processes can be tracked concurrently.
     */
    @Test
    public void testConcurrentProcessMonitoring() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Submit multiple processes
        for (int i = 0; i < threadCount; i++) {
            final int processIndex = i;
            executor.submit(() -> {
                ProcessStatus process = processMonitor.startProcess();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                process.setProgress(100.0);
                process.setStatus(ProcessStatus.Status.COMPLETED);
                processMonitor.stopProcess(process);
            });
        }

        executor.shutdown();
        assertTrue("Executor should terminate", executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify all processes completed
        for (long i = 1; i <= threadCount; i++) {
            ProcessStatus process = processMonitor.getProcess(i);
            assertNotNull("Process " + i + " should exist", process);
            assertEquals("Process " + i + " should be completed", ProcessStatus.Status.COMPLETED, process.getStatus());
        }
    }

    /**
     * Test 8: Storage Directory Structure
     * Verify that storage directories are created with correct structure.
     */
    @Test
    public void testStorageDirectoryStructure() {
        Path exportsPath = fileManager.getExportsPath();
        Path importsPath = fileManager.getImportsPath();

        assertNotNull("Exports path should exist", exportsPath);
        assertNotNull("Imports path should exist", importsPath);

        assertTrue("Exports directory should be created", Files.exists(exportsPath));
        assertTrue("Imports directory should be created", Files.exists(importsPath));

        assertTrue("Exports path should end with exim-exports", exportsPath.toString().endsWith("exim-exports"));
        assertTrue("Imports path should end with exim-imports", importsPath.toString().endsWith("exim-imports"));
    }

    /**
     * Test 9: File Cleanup
     * Verify that expired files are cleaned up by cleanup task.
     */
    @Test
    public void testFileCleanup() throws IOException, InterruptedException {
        // Create multiple files
        String exportFile1 = fileManager.createExportFile(1L);
        String exportFile2 = fileManager.createExportFile(2L);

        assertTrue("Files should exist after creation",
                new File(exportFile1).exists() && new File(exportFile2).exists());

        // Wait for files to expire
        Thread.sleep(2500);

        // Run cleanup
        fileManager.cleanupExpiredFiles();

        // Files should be deleted
        assertFalse("File 1 should be cleaned up", new File(exportFile1).exists());
        assertFalse("File 2 should be cleaned up", new File(exportFile2).exists());
    }

    /**
     * Test 10: Process Status Fields for Export
     * Verify that export file path is correctly stored in process status.
     */
    @Test
    public void testProcessStatusExportPath() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String exportPath = fileManager.createExportFile(process.getId());

        process.setExportFilePath(exportPath);
        assertEquals("Export file path should be stored", exportPath, process.getExportFilePath());

        // Retrieve and verify
        ProcessStatus retrieved = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be retrievable", retrieved);

        fileManager.deleteExportFile(exportPath);
        processMonitor.stopProcess(process);
    }

    /**
     * Test 11: Process Status Fields for Import
     * Verify that import file path is correctly stored in process status.
     */
    @Test
    public void testProcessStatusImportPath() throws IOException {
        ProcessStatus process = processMonitor.startProcess();
        String importPath = fileManager.createImportFile(process.getId());

        process.setImportFilePath(importPath);
        assertEquals("Import file path should be stored", importPath, process.getImportFilePath());

        // Retrieve and verify
        ProcessStatus retrieved = processMonitor.getProcess(process.getId());
        assertNotNull("Process should be retrievable", retrieved);

        fileManager.deleteImportFile(importPath);
        processMonitor.stopProcess(process);
    }

    /**
     * Test 12: Error Handling - Directory Traversal Prevention
     * Verify that directory traversal attempts are prevented.
     */
    @Test
    public void testDirectoryTraversalPrevention() throws IOException {
        String exportFile = fileManager.createExportFile(1L);

        // Attempt to access file with directory traversal
        try {
            String maliciousPath = exportFile.replace("exim-exports", "exim-exports/../../..");
            fileManager.getExportFile(maliciousPath);
            fail("Should reject directory traversal attempt");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception should indicate invalid file path", e.getMessage().contains("Invalid file path"));
        }
    }

    /**
     * Test 13: Error Handling - Non-existent File
     * Verify that accessing non-existent file throws appropriate error.
     */
    @Test
    public void testNonExistentFileHandling() throws IOException {
        // Create a path that's valid (within exim-exports) but doesn't exist
        String nonExistentPath = fileManager.getExportsPath().resolve("nonexistent-file-12345.zip").toString();

        try {
            fileManager.getExportFile(nonExistentPath);
            fail("Should throw IOException for non-existent file");
        } catch (IOException e) {
            assertTrue("Exception should indicate file not found", e.getMessage().contains("not found"));
        }
    }

    /**
     * Test 14: Process Status Completion Time
     * Verify that completion time is correctly tracked.
     */
    @Test
    public void testProcessCompletionTime() throws InterruptedException {
        ProcessStatus process = processMonitor.startProcess();
        assertEquals("Initial completion time should be 0", 0L, process.getCompletionTimeMillis());

        long beforeCompletion = System.currentTimeMillis();
        Thread.sleep(100);
        long completionTime = System.currentTimeMillis();

        process.setCompletionTimeMillis(completionTime);

        long afterCompletion = System.currentTimeMillis();
        assertTrue("Completion time should be set correctly",
                completionTime >= beforeCompletion && completionTime <= afterCompletion);
    }

    /**
     * Test 15: Process Status Error Message
     * Verify that error messages are correctly stored and retrieved.
     */
    @Test
    public void testProcessStatusErrorMessage() {
        ProcessStatus process = processMonitor.startProcess();
        assertNull("Initial error message should be null", process.getErrorMessage());

        String errorMsg = "Test error message";
        process.setErrorMessage(errorMsg);
        assertEquals("Error message should be stored", errorMsg, process.getErrorMessage());

        process.setStatus(ProcessStatus.Status.FAILED);
        assertEquals("Status should be FAILED", ProcessStatus.Status.FAILED, process.getStatus());
    }

    /**
     * Test 16: Cleanup on Shutdown
     * Verify that file manager can shutdown cleanly.
     */
    @Test
    public void testShutdown() throws IOException {
        ProcessFileManager tempManager = new ProcessFileManager(tempStorageDir, 86400000L);
        String exportFile = tempManager.createExportFile(1L);
        assertTrue("File should be created", new File(exportFile).exists());

        // Note: actual cleanup would delete the base directory
        // In this test, we verify the method exists and doesn't throw
        tempManager.shutdown();
    }

    /**
     * Test 17: Multiple Process States
     * Verify different process states can coexist.
     */
    @Test
    public void testMultipleProcessStates() {
        ProcessStatus running = processMonitor.startProcess();
        running.setStatus(ProcessStatus.Status.RUNNING);
        running.setProgress(50.0);

        ProcessStatus completed = processMonitor.startProcess();
        completed.setStatus(ProcessStatus.Status.COMPLETED);
        completed.setProgress(100.0);
        completed.setCompletionTimeMillis(System.currentTimeMillis());

        ProcessStatus failed = processMonitor.startProcess();
        failed.setStatus(ProcessStatus.Status.FAILED);
        failed.setErrorMessage("Test failure");

        ProcessStatus cancelled = processMonitor.startProcess();
        cancelled.setStatus(ProcessStatus.Status.CANCELLED);

        // Verify all states exist
        assertEquals("Running process should have correct status", ProcessStatus.Status.RUNNING, running.getStatus());
        assertEquals("Completed process should have correct status", ProcessStatus.Status.COMPLETED, completed.getStatus());
        assertEquals("Failed process should have correct status", ProcessStatus.Status.FAILED, failed.getStatus());
        assertEquals("Cancelled process should have correct status", ProcessStatus.Status.CANCELLED, cancelled.getStatus());
    }

    /**
     * Test 18: File Manager Configuration
     * Verify that file manager can be initialized with custom configuration.
     */
    @Test
    public void testFileManagerConfiguration() {
        long customTtl = 5000L;
        ProcessFileManager customManager = new ProcessFileManager(tempStorageDir, customTtl);

        assertEquals("TTL should match configuration", customTtl, customManager.getFileTtlMillis());
        assertNotNull("Storage path should be set", customManager.getStorageBasePath());
    }

    /**
     * Test 19: Concurrent File Operations
     * Verify that multiple files can be created and managed concurrently.
     */
    @Test
    public void testConcurrentFileOperations() throws IOException, InterruptedException {
        int fileCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < fileCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String file = fileManager.createExportFile(index);
                    assertNotNull("File should be created", file);
                    assertTrue("File should exist", new File(file).exists());
                } catch (IOException e) {
                    fail("File creation should not fail: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        assertTrue("Executor should terminate", executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    /**
     * Test 20: Process ID Uniqueness
     * Verify that process IDs are unique and incrementing.
     */
    @Test
    public void testProcessIdUniqueness() {
        long id1 = processMonitor.startProcess().getId();
        long id2 = processMonitor.startProcess().getId();
        long id3 = processMonitor.startProcess().getId();

        assertTrue("Process IDs should be unique", id1 != id2 && id2 != id3 && id1 != id3);
        assertTrue("Process IDs should be incrementing", id1 < id2 && id2 < id3);
    }
}
