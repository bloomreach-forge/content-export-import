/*
 * Copyright 2022 Bloomreach B.V. (https://www.bloomreach.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

/**
 * DaemonModule implementation to register Content EXIM export and import JAX-RS services.
 */
public class ContentEximJaxrsDaemonModule extends AbstractReconfigurableDaemonModule {

    private static final String DEFAULT_END_POINT = "/exim";

    private String modulePath;
    private String endpoint;

    private ProcessMonitor processMonitor;

    private ContentEximProcessStatusService contentEximProcessStatusService;
    private ContentEximExportService contentEximExportService;
    private ContentEximImportService contentEximImportService;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        modulePath = moduleConfig.getParent().getPath();
        endpoint = JcrUtils.getStringProperty(moduleConfig, "endpoint", DEFAULT_END_POINT);
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        processMonitor = new ProcessMonitor();

        contentEximProcessStatusService = new ContentEximProcessStatusService();
        contentEximExportService = new ContentEximExportService();
        contentEximImportService = new ContentEximImportService();

        contentEximProcessStatusService.setProcessMonitor(processMonitor);
        contentEximExportService.setProcessMonitor(processMonitor);
        contentEximImportService.setProcessMonitor(processMonitor);

        contentEximProcessStatusService.setDaemonSession(session);
        contentEximExportService.setDaemonSession(session);
        contentEximImportService.setDaemonSession(session);

        RepositoryJaxrsService.addEndpoint(
                new RepositoryJaxrsEndpoint(endpoint)
                .singleton(contentEximProcessStatusService)
                .singleton(contentEximExportService)
                .singleton(contentEximImportService)
                .authorized(modulePath, RepositoryJaxrsService.HIPPO_REST_PERMISSION));
    }

    @Override
    protected void doShutdown() {
        RepositoryJaxrsService.removeEndpoint(endpoint);
    }

}
