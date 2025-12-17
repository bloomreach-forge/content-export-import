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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ProcessFileManager;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DaemonModule implementation to register Content EXIM export and import JAX-RS services.
 */
public class ContentEximJaxrsDaemonModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(ContentEximJaxrsDaemonModule.class);

    private static final String DEFAULT_END_POINT = "/exim";
    private static final int DEFAULT_ASYNC_THREAD_POOL_SIZE = 5;
    private static final long DEFAULT_CLEANUP_INTERVAL_MINUTES = 60;

    private String modulePath;
    private String endpoint;
    private String storageDir;
    private Long fileTtlMillis;
    private int asyncThreadPoolSize = DEFAULT_ASYNC_THREAD_POOL_SIZE;
    private long cleanupIntervalMinutes = DEFAULT_CLEANUP_INTERVAL_MINUTES;

    private ProcessMonitor processMonitor;
    private ProcessFileManager fileManager;
    private ExecutorService asyncExecutor;
    private ScheduledExecutorService cleanupExecutor;

    private ContentEximProcessStatusService contentEximProcessStatusService;
    private ContentEximExportService contentEximExportService;
    private ContentEximImportService contentEximImportService;
    private ContentEximAsyncExportService contentEximAsyncExportService;
    private ContentEximAsyncImportService contentEximAsyncImportService;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        modulePath = moduleConfig.getParent().getPath();
        endpoint = JcrUtils.getStringProperty(moduleConfig, "endpoint", DEFAULT_END_POINT);
        storageDir = JcrUtils.getStringProperty(moduleConfig, "storageDir", null);
        fileTtlMillis = JcrUtils.getLongProperty(moduleConfig, "fileTtlMillis", null);
        asyncThreadPoolSize = (int)(long) JcrUtils.getLongProperty(moduleConfig, "asyncThreadPoolSize",
                (long) DEFAULT_ASYNC_THREAD_POOL_SIZE);
        cleanupIntervalMinutes = JcrUtils.getLongProperty(moduleConfig, "cleanupIntervalMinutes",
                (long) DEFAULT_CLEANUP_INTERVAL_MINUTES);
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        try {
            // Initialize ProcessMonitor
            processMonitor = new ProcessMonitor();

            // Initialize ProcessFileManager
            fileManager = new ProcessFileManager(storageDir, fileTtlMillis);

            // Initialize ExecutorService for async operations
            asyncExecutor = Executors.newFixedThreadPool(asyncThreadPoolSize);
            log.info("Initialized async executor with {} threads", asyncThreadPoolSize);

            // Initialize ScheduledExecutorService for cleanup tasks
            cleanupExecutor = Executors.newScheduledThreadPool(1);
            cleanupExecutor.scheduleAtFixedRate(() -> {
                try {
                    fileManager.cleanupExpiredFiles();
                } catch (Exception e) {
                    log.error("Error during file cleanup", e);
                }
            }, cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
            log.info("Initialized cleanup scheduler with interval {} minutes", cleanupIntervalMinutes);

            // Initialize synchronous services
            contentEximProcessStatusService = new ContentEximProcessStatusService();
            contentEximExportService = new ContentEximExportService();
            contentEximImportService = new ContentEximImportService();

            contentEximProcessStatusService.setProcessMonitor(processMonitor);
            contentEximExportService.setProcessMonitor(processMonitor);
            contentEximImportService.setProcessMonitor(processMonitor);

            contentEximProcessStatusService.setDaemonSession(session);
            contentEximExportService.setDaemonSession(session);
            contentEximImportService.setDaemonSession(session);

            // Initialize async services
            contentEximAsyncExportService = new ContentEximAsyncExportService();
            contentEximAsyncImportService = new ContentEximAsyncImportService();

            contentEximAsyncExportService.setProcessMonitor(processMonitor);
            contentEximAsyncImportService.setProcessMonitor(processMonitor);

            contentEximAsyncExportService.setDaemonSession(session);
            contentEximAsyncImportService.setDaemonSession(session);

            contentEximAsyncExportService.setFileManager(fileManager);
            contentEximAsyncImportService.setFileManager(fileManager);

            contentEximAsyncExportService.setExecutorService(asyncExecutor);
            contentEximAsyncImportService.setExecutorService(asyncExecutor);

            contentEximAsyncExportService.setExportService(contentEximExportService);
            contentEximAsyncImportService.setImportService(contentEximImportService);

            // Register endpoints
            RepositoryJaxrsService.addEndpoint(
                    new RepositoryJaxrsEndpoint(endpoint)
                    .singleton(contentEximProcessStatusService)
                    .singleton(contentEximExportService)
                    .singleton(contentEximImportService)
                    .singleton(contentEximAsyncExportService)
                    .singleton(contentEximAsyncImportService)
                    .authorized(modulePath, RepositoryJaxrsService.HIPPO_REST_PERMISSION));

            log.info("ContentEximJaxrsDaemonModule initialized successfully with endpoint: {}", endpoint);

        } catch (Exception e) {
            log.error("Failed to initialize ContentEximJaxrsDaemonModule", e);
            throw new RepositoryException("Failed to initialize EXIM module", e);
        }
    }

    @Override
    protected void doShutdown() {
        try {
            RepositoryJaxrsService.removeEndpoint(endpoint);

            if (cleanupExecutor != null) {
                cleanupExecutor.shutdown();
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            }

            if (asyncExecutor != null) {
                asyncExecutor.shutdown();
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            }

            if (fileManager != null) {
                fileManager.shutdown();
            }

            log.info("ContentEximJaxrsDaemonModule shutdown completed");

        } catch (Exception e) {
            log.error("Error during ContentEximJaxrsDaemonModule shutdown", e);
        }
    }

}
