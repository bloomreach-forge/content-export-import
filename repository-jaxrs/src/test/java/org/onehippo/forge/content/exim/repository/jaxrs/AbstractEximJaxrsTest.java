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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for Content-EXIM JAX-RS service tests.
 * Provides common setup, helpers, and assertion methods.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractEximJaxrsTest extends AbstractJaxrsTest {

    protected static final int HTTP_OK = 200;
    protected static final int HTTP_SERVER_ERROR = 500;

    protected static final String DOCS_PATH = "/content/documents/exim";
    protected static final String BINARIES_PATH = "/content/gallery/exim";
    protected static final String DOCS_QUERY = "/jcr:root/content/documents/exim//element(*,hippo:document)";
    protected static final String BINARIES_QUERY = "/jcr:root/content/gallery//element(*,hippo:document)";

    private static final String BOUNDARY = "----TestBoundary7MA4YWxkTrZu0gW";
    private static final String MULTIPART_CONTENT_TYPE = "multipart/form-data; boundary=" + BOUNDARY;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected Session session;

    @BeforeAll
    public void init() {
        super.init();
    }

    @AfterAll
    public void destroy() {
        if (session != null && session.isLive()) {
            session.logout();
        }
        super.destroy();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        setupForNewRequest();

        Repository repository = getComponentManager().getComponent(Repository.class);
        session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        initService();

        getHstRequest().setMethod("POST");
        getHstRequest().setRequestURI(getEndpointUri());
    }

    /**
     * Initialize service-specific components (e.g., set daemon session).
     */
    protected abstract void initService() throws Exception;

    /**
     * Get the endpoint URI for this service (e.g., "/site/api/import/" or "/site/api/export/").
     */
    protected abstract String getEndpointUri();

    @Override
    protected String getAnnotatedHstBeansClasses() {
        return "classpath*:org/onehippo/forge/content/exim/**/*.class";
    }

    @Override
    protected List<String> contributeSpringConfigurationLocations() {
        return Arrays.asList("/brut-test-config.xml", "/rest-resources.xml");
    }

    @Override
    protected String contributeHstConfigurationRootPath() {
        return "/hst:exim";
    }

    @Override
    protected List<String> contributeAddonModulePaths() {
        return Arrays.asList();
    }

    // ========== ExecutionParams Builders ==========

    protected ExecutionParams docsParams() {
        return paramsWithDocIncludes(DOCS_PATH);
    }

    protected ExecutionParams binariesParams() {
        return paramsWithBinaryIncludes(BINARIES_PATH);
    }

    protected ExecutionParams docsQueryParams() {
        return paramsWithDocQueries(DOCS_QUERY);
    }

    protected ExecutionParams binariesQueryParams() {
        return paramsWithBinaryQueries(BINARIES_QUERY);
    }

    protected ExecutionParams paramsWithDocIncludes(String... paths) {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setIncludes(Arrays.asList(paths));
        params.setDocuments(docs);
        return params;
    }

    protected ExecutionParams paramsWithBinaryIncludes(String... paths) {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setIncludes(Arrays.asList(paths));
        params.setBinaries(binaries);
        return params;
    }

    protected ExecutionParams paramsWithDocQueries(String... queries) {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setQueries(Arrays.asList(queries));
        params.setDocuments(docs);
        return params;
    }

    protected ExecutionParams paramsWithBinaryQueries(String... queries) {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setQueries(Arrays.asList(queries));
        params.setBinaries(binaries);
        return params;
    }

    protected String toJson(ExecutionParams params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Request Setup Helpers ==========

    protected void setupRequest(ExecutionParams params) throws IOException {
        setupRequest(toJson(params), null);
    }

    protected void setupRequest(ExecutionParams params, Map<String, String> additionalParams) throws IOException {
        setupRequest(toJson(params), additionalParams);
    }

    protected void setupRequest(String paramsJson) throws IOException {
        setupRequest(paramsJson, null);
    }

    protected void setupRequest(String paramsJson, Map<String, String> additionalParams) throws IOException {
        getHstRequest().setHeader("Content-Type", MULTIPART_CONTENT_TYPE);
        byte[] multipartBody = buildMultipartBody(paramsJson, additionalParams, createEmptyZipBytes());
        getHstRequest().setInputStream(createServletInputStream(multipartBody));
    }

    protected void setupFormRequest(String paramsJson) {
        getHstRequest().setHeader("Content-Type", "multipart/form-data");
        if (paramsJson != null) {
            getHstRequest().addParameter("paramsJson", paramsJson);
        }
    }

    protected void setupFormRequest(String paramsJson, Map<String, String> additionalParams) {
        setupFormRequest(paramsJson);
        if (additionalParams != null) {
            additionalParams.forEach((k, v) -> getHstRequest().addParameter(k, v));
        }
    }

    // ========== Assertion Helpers ==========

    protected String invokeAndAssertValid() throws Exception {
        String response = invokeFilter();
        assertNotNull(response);
        assertNoUnexpectedException(response);
        return response;
    }

    protected void assertNoUnexpectedException(String response) {
        if (response != null) {
            assertFalse(response.contains("NullPointerException"),
                "Unexpected NullPointerException in response: " + truncate(response, 200));
            assertFalse(response.contains("ClassCastException"),
                "Unexpected ClassCastException in response: " + truncate(response, 200));
            if (response.contains("IllegalStateException") && !response.contains("multipart")
                    && !response.contains("boundary") && !response.contains("Attachment")) {
                fail("Unexpected IllegalStateException in response: " + truncate(response, 200));
            }
        }
    }

    protected String truncate(String s, int maxLen) {
        return s == null ? null : (s.length() <= maxLen ? s : s.substring(0, maxLen) + "...");
    }

    // ========== Multipart Body Builders ==========

    private byte[] buildMultipartBody(String paramsJson, Map<String, String> additionalParams, byte[] zipContent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String lineEnd = "\r\n";

        if (paramsJson != null) {
            baos.write(("--" + BOUNDARY + lineEnd).getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"paramsJson\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes(StandardCharsets.UTF_8));
            baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            baos.write(paramsJson.getBytes(StandardCharsets.UTF_8));
            baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
        }

        if (additionalParams != null) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                baos.write(("--" + BOUNDARY + lineEnd).getBytes(StandardCharsets.UTF_8));
                baos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
                baos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes(StandardCharsets.UTF_8));
                baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
                baos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
            }
        }

        baos.write(("--" + BOUNDARY + lineEnd).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"package\"; filename=\"import.zip\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: application/zip" + lineEnd).getBytes(StandardCharsets.UTF_8));
        baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));
        baos.write(zipContent);
        baos.write(lineEnd.getBytes(StandardCharsets.UTF_8));

        baos.write(("--" + BOUNDARY + "--" + lineEnd).getBytes(StandardCharsets.UTF_8));

        return baos.toByteArray();
    }

    private byte[] createEmptyZipBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("empty.txt"));
            zos.write("empty".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private ServletInputStream createServletInputStream(byte[] content) {
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }
}
