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
package org.onehippo.forge.content.exim.repository.jaxrs.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages file lifecycle for asynchronous export/import operations.
 * Handles file creation, retrieval, and cleanup based on configured policies.
 */
public class ProcessFileManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessFileManager.class);

    private static final String ENV_VAR_NAME = "EXIM_STORAGE_DIR";
    private static final String SYSTEM_PROPERTY_NAME = "exim.storage.dir";
    private static final String DEFAULT_EXPORT_SUBDIR = "exim-exports";
    private static final String DEFAULT_IMPORT_SUBDIR = "exim-imports";
    private static final long DEFAULT_FILE_TTL_MILLIS = 24 * 60 * 60 * 1000; // 24 hours

    private final Path storageBasePath;
    private final Path exportsPath;
    private final Path importsPath;
    private final long fileTtlMillis;

    public ProcessFileManager(String configuredStorageDir, Long configuredFileTtlMillis) {
        this.fileTtlMillis = configuredFileTtlMillis != null ? configuredFileTtlMillis : DEFAULT_FILE_TTL_MILLIS;
        this.storageBasePath = resolveStorageBasePath(configuredStorageDir);
        this.exportsPath = storageBasePath.resolve(DEFAULT_EXPORT_SUBDIR);
        this.importsPath = storageBasePath.resolve(DEFAULT_IMPORT_SUBDIR);

        try {
            Files.createDirectories(exportsPath);
            Files.createDirectories(importsPath);
            log.info("ProcessFileManager initialized with base path: {}", storageBasePath);
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
            throw new RuntimeException("Failed to initialize ProcessFileManager", e);
        }
    }

    /**
     * Resolves the storage base path based on configuration hierarchy:
     * 1. Environment variable (EXIM_STORAGE_DIR)
     * 2. Module configuration parameter
     * 3. System property (exim.storage.dir)
     * 4. Default temp directory
     */
    private Path resolveStorageBasePath(String configuredStorageDir) {
        String storagePath = null;

        // Priority 1: Environment variable
        String envPath = System.getenv(ENV_VAR_NAME);
        if (StringUtils.isNotBlank(envPath)) {
            storagePath = envPath;
            log.debug("Using storage path from environment variable: {}", envPath);
        }

        // Priority 2: Module configuration
        if (storagePath == null && StringUtils.isNotBlank(configuredStorageDir)) {
            storagePath = configuredStorageDir;
            log.debug("Using storage path from module configuration: {}", configuredStorageDir);
        }

        // Priority 3: System property
        if (storagePath == null) {
            String sysPropPath = System.getProperty(SYSTEM_PROPERTY_NAME);
            if (StringUtils.isNotBlank(sysPropPath)) {
                storagePath = sysPropPath;
                log.debug("Using storage path from system property: {}", sysPropPath);
            }
        }

        // Priority 4: Default temp directory
        if (storagePath == null) {
            storagePath = System.getProperty("java.io.tmpdir") + File.separator + "exim-async";
            log.debug("Using default storage path: {}", storagePath);
        }

        return Paths.get(storagePath);
    }

    /**
     * Creates a new export file with a unique identifier.
     * Returns the file path relative to the exports directory.
     */
    public String createExportFile(String processId) throws IOException {
        String filename = String.format("exim-export-%s-%s.zip", processId, UUID.randomUUID().toString());
        Path filePath = exportsPath.resolve(filename);
        Files.createFile(filePath);
        return filePath.toString();
    }

    /**
     * Creates a new import file with a unique identifier.
     * Returns the file path relative to the imports directory.
     */
    public String createImportFile(String processId) throws IOException {
        String filename = String.format("exim-import-%s-%s.zip", processId, UUID.randomUUID().toString());
        Path filePath = importsPath.resolve(filename);
        Files.createFile(filePath);
        return filePath.toString();
    }

    /**
     * Retrieves a file for reading/download.
     * Verifies file exists and hasn't expired.
     */
    public File getExportFile(String filePath) throws IOException {
        return getFile(filePath, exportsPath);
    }

    /**
     * Retrieves an import file for reading.
     */
    public File getImportFile(String filePath) throws IOException {
        return getFile(filePath, importsPath);
    }

    private File getFile(String filePath, Path validationBasePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        Path requestedPath = Paths.get(filePath);

        // Validate path to prevent directory traversal attacks
        if (!requestedPath.normalize().startsWith(validationBasePath.normalize())) {
            throw new IllegalArgumentException("Invalid file path");
        }

        File file = requestedPath.toFile();
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        // Check if file has expired
        if (isFileExpired(file)) {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                log.warn("Failed to delete expired file: {}", filePath, e);
            }
            throw new IOException("File has expired: " + filePath);
        }

        return file;
    }

    /**
     * Deletes an export file.
     */
    public void deleteExportFile(String filePath) throws IOException {
        deleteFile(filePath, exportsPath);
    }

    /**
     * Deletes an import file.
     */
    public void deleteImportFile(String filePath) throws IOException {
        deleteFile(filePath, importsPath);
    }

    private void deleteFile(String filePath, Path validationBasePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        Path requestedPath = Paths.get(filePath);

        // Validate path to prevent directory traversal attacks
        if (!requestedPath.normalize().startsWith(validationBasePath.normalize())) {
            throw new IllegalArgumentException("Invalid file path");
        }

        File file = requestedPath.toFile();
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }
    }

    /**
     * Performs cleanup of expired files in export and import directories.
     * Called periodically by a cleanup task.
     */
    public void cleanupExpiredFiles() {
        cleanupExpiredFilesInDirectory(exportsPath);
        cleanupExpiredFilesInDirectory(importsPath);
    }

    private void cleanupExpiredFilesInDirectory(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isFileExpired(path.toFile()))
                    .forEach(path -> {
                        try {
                            FileUtils.forceDelete(path.toFile());
                            log.debug("Deleted expired file: {}", path);
                        } catch (IOException e) {
                            log.warn("Failed to delete expired file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to cleanup expired files in directory: {}", directory, e);
        }
    }

    /**
     * Checks if a file has exceeded its TTL.
     */
    private boolean isFileExpired(File file) {
        if (!file.exists()) {
            return true;
        }
        long fileAge = System.currentTimeMillis() - file.lastModified();
        return fileAge > fileTtlMillis;
    }

    /**
     * Cleans up all storage directories when shutting down.
     */
    public void shutdown() {
        try {
            FileUtils.deleteDirectory(storageBasePath.toFile());
            log.info("ProcessFileManager shutdown completed");
        } catch (IOException e) {
            log.warn("Failed to cleanup storage directories during shutdown", e);
        }
    }

    /**
     * Gets the configured file TTL in milliseconds.
     */
    public long getFileTtlMillis() {
        return fileTtlMillis;
    }

    /**
     * Gets the storage base path.
     */
    public Path getStorageBasePath() {
        return storageBasePath;
    }

    /**
     * Gets the exports directory path.
     */
    public Path getExportsPath() {
        return exportsPath;
    }

    /**
     * Gets the imports directory path.
     */
    public Path getImportsPath() {
        return importsPath;
    }
}
