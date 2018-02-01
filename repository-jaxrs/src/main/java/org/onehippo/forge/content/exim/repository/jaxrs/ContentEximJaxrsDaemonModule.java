/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

public class ContentEximJaxrsDaemonModule extends AbstractReconfigurableDaemonModule {

    private static final String DEFAULT_END_POINT = "/exim";

    private String modulePath;

    private ContentEximExportService contentEximExportService;
    private ContentEximImportService contentEximImportService;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        modulePath = moduleConfig.getParent().getPath();
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        contentEximExportService = new ContentEximExportService();
        contentEximImportService = new ContentEximImportService();

        contentEximExportService.setDaemonSession(session);
        contentEximImportService.setDaemonSession(session);

        RepositoryJaxrsService.addEndpoint(
                new RepositoryJaxrsEndpoint(DEFAULT_END_POINT)
                .singleton(contentEximExportService)
                .singleton(contentEximImportService)
                .authorized(modulePath, RepositoryJaxrsService.HIPPO_REST_PERMISSION));
    }

    @Override
    protected void doShutdown() {
        RepositoryJaxrsService.removeEndpoint(DEFAULT_END_POINT);
    }

}
