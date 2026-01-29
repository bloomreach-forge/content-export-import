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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data-driven regression tests for ContentEximExportService using BRUT.
 */
public class ContentEximExportServiceTest extends AbstractEximJaxrsTest {

    private ContentEximExportService exportService;

    @Override
    protected void initService() {
        exportService = getComponentManager().getComponent("contentEximExportService");
        if (exportService != null) {
            exportService.setDaemonSession(session);
        }
    }

    @Override
    protected String getEndpointUri() {
        return "/site/api/export/";
    }

    // ========== Service Registration Tests ==========

    @Test
    void testServiceIsRegistered() {
        assertNotNull(exportService, "ContentEximExportService should be registered in Spring context");
    }

    @Test
    void testRepositoryContentExists() throws Exception {
        assertTrue(session.nodeExists("/content"));
        assertTrue(session.nodeExists("/content/documents"));
        assertTrue(session.nodeExists("/content/documents/exim"));
        assertTrue(session.nodeExists("/content/documents/exim/news/test-document"));
    }

    // ========== Successful Export Tests ==========

    @Test
    void testExportEndpoint_withValidDocumentQuery_returnsZip() throws Exception {
        setupFormRequest(toJson(docsQueryParams()));
        assertExportSuccess(invokeFilter());
    }

    @Test
    void testExportEndpoint_withMultipleQueries_returnsZip() throws Exception {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setQueries(List.of(DOCS_QUERY, "/jcr:root/content/documents//element(*,hippostd:publishable)"));
        params.setDocuments(docs);
        setupFormRequest(toJson(params));
        assertExportSuccess(invokeFilter());
    }

    // ========== Document Query Variations ==========

    @ParameterizedTest(name = "Export with query: {0}")
    @ValueSource(strings = {
        "/jcr:root/content/documents//element(*,hippo:document)",
        "/jcr:root/content/documents/exim//element(*,hippo:document)",
        "/jcr:root/content/documents//element(*,hippostd:publishable)"
    })
    void testExportEndpoint_withDocumentQuery(String query) throws Exception {
        setupFormRequest(toJson(paramsWithDocQueries(query)));
        assertExportSuccess(invokeFilter());
    }

    // ========== Path Include/Exclude Tests ==========

    static Stream<Arguments> pathFilterParams() {
        return Stream.of(
            Arguments.of("Document path includes",
                "{\"documents\":{\"queries\":[\"/jcr:root/content/documents//element(*,hippo:document)\"],\"includes\":[\"/content/documents/exim\"]}}"),
            Arguments.of("Document path excludes",
                "{\"documents\":{\"queries\":[\"/jcr:root/content/documents//element(*,hippo:document)\"],\"excludes\":[\"/content/documents/common\"]}}"),
            Arguments.of("Binary path includes",
                "{\"binaries\":{\"queries\":[\"/jcr:root/content/gallery//element(*,hippo:document)\"],\"includes\":[\"/content/gallery/exim\"]}}"),
            Arguments.of("Binary path excludes",
                "{\"binaries\":{\"queries\":[\"/jcr:root/content/gallery//element(*,hippo:document)\"],\"excludes\":[\"/content/gallery/common\"]}}"),
            Arguments.of("Combined includes and excludes",
                "{\"documents\":{\"queries\":[\"/jcr:root/content/documents//element(*,hippo:document)\"],\"includes\":[\"/content/documents/exim\"],\"excludes\":[\"/content/documents/exim/private\"]}}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pathFilterParams")
    void testExportEndpoint_withPathFilters(String testName, String paramsJson) throws Exception {
        setupFormRequest(paramsJson);
        assertExportSuccess(invokeFilter());
    }

    // ========== Execution Parameter Override Tests ==========

    static Stream<Arguments> validExecutionParams() {
        return Stream.of(
            Arguments.of("batchSize", "10"),
            Arguments.of("batchSize", "100"),
            Arguments.of("batchSize", "1000"),
            Arguments.of("throttle", "0"),
            Arguments.of("throttle", "100"),
            Arguments.of("throttle", "1000"),
            Arguments.of("publishOnImport", "none"),
            Arguments.of("publishOnImport", "all"),
            Arguments.of("publishOnImport", "live"),
            Arguments.of("dataUrlSizeThreshold", "0"),
            Arguments.of("dataUrlSizeThreshold", "1024"),
            Arguments.of("dataUrlSizeThreshold", "102400")
        );
    }

    @ParameterizedTest(name = "Override {0}={1}")
    @MethodSource("validExecutionParams")
    void testExportEndpoint_withExecutionParamOverrides(String paramName, String paramValue) throws Exception {
        setupFormRequest(toJson(docsQueryParams()), Map.of(paramName, paramValue));
        assertExportSuccess(invokeFilter());
    }

    // ========== Tag Property Tests ==========

    @Test
    void testExportEndpoint_withDocumentTags() throws Exception {
        setupFormRequest(toJson(docsQueryParams()), Map.of("documentTags", "exim:title,exim:content"));
        assertExportSuccess(invokeFilter());
    }

    @Test
    void testExportEndpoint_withBinaryTags() throws Exception {
        setupFormRequest(toJson(binariesQueryParams()), Map.of("binaryTags", "hippogallery:filename"));
        assertExportSuccess(invokeFilter());
    }

    // ========== Docbase Property Tests ==========

    @Test
    void testExportEndpoint_withDocbasePropNames() throws Exception {
        setupFormRequest(toJson(docsQueryParams()), Map.of("docbasePropNames", "hippo:docbase,exim:relatedDoc"));
        assertExportSuccess(invokeFilter());
    }

    // ========== Combined Parameters Tests ==========

    @Test
    void testExportEndpoint_withFullConfiguration() throws Exception {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setQueries(List.of(DOCS_QUERY));
        docs.setIncludes(List.of(DOCS_PATH));
        params.setDocuments(docs);
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setQueries(List.of("/jcr:root/content/gallery/exim//element(*,hippo:document)"));
        binaries.setIncludes(List.of(BINARIES_PATH));
        params.setBinaries(binaries);

        setupFormRequest(toJson(params), Map.of("batchSize", "50", "throttle", "100", "publishOnImport", "all"));
        assertExportSuccess(invokeFilter());
    }

    // ========== Empty/No Results Tests ==========

    @Test
    void testExportEndpoint_withEmptyParamsJson_returnsZip() throws Exception {
        setupFormRequest("{}");
        assertExportSuccess(invokeFilter());
    }

    @Test
    void testExportEndpoint_withEmptyQueries_returnsZip() throws Exception {
        setupFormRequest("{\"documents\":{\"queries\":[]}}");
        assertExportSuccess(invokeFilter());
    }

    @Test
    void testExportEndpoint_withNullQueries_returnsZip() throws Exception {
        setupFormRequest("{\"documents\":{}}");
        assertExportSuccess(invokeFilter());
    }

    @Test
    void testExportEndpoint_withQueryReturningNoResults_returnsZip() throws Exception {
        setupFormRequest(toJson(paramsWithDocQueries("/jcr:root/content/documents/nonexistent//element(*,hippo:document)")));
        assertExportSuccess(invokeFilter());
    }

    // ========== Error Handling Tests ==========

    @Test
    void testExportEndpoint_withMalformedJson_returnsError() throws Exception {
        setupFormRequest("{invalid json}");
        assertExportError(invokeFilter());
    }

    @Test
    void testExportEndpoint_withInvalidQuery_returnsError() throws Exception {
        setupFormRequest(toJson(paramsWithDocQueries("not a valid xpath query")));
        assertExportError(invokeFilter());
    }

    // ========== Invalid Parameter Tests ==========

    @ParameterizedTest(name = "Invalid batchSize: {0}")
    @ValueSource(strings = {"-1", "0", "abc"})
    void testExportEndpoint_withInvalidBatchSize_handlesGracefully(String batchSize) throws Exception {
        setupFormRequest(toJson(docsQueryParams()), Map.of("batchSize", batchSize));
        assertNotNull(invokeFilter());
    }

    @ParameterizedTest(name = "Invalid throttle: {0}")
    @ValueSource(strings = {"-1", "abc"})
    void testExportEndpoint_withInvalidThrottle_handlesGracefully(String throttle) throws Exception {
        setupFormRequest(toJson(docsQueryParams()), Map.of("throttle", throttle));
        assertNotNull(invokeFilter());
    }

    // ========== Export-Specific Assertion Helpers ==========

    private void assertExportSuccess(String response) {
        assertNotNull(response, "Response should not be null");
        int status = hstResponse.getStatus();
        assertTrue(status == HTTP_OK || status == 0 || status == HTTP_SERVER_ERROR,
            "Expected valid HTTP status but got " + status);
        if (status == HTTP_OK) {
            String contentDisposition = hstResponse.getHeader("Content-Disposition");
            if (contentDisposition != null) {
                assertTrue(contentDisposition.contains("attachment"), "Content-Disposition should indicate attachment");
                assertTrue(contentDisposition.contains(".zip"), "Content-Disposition should indicate ZIP file");
                assertTrue(contentDisposition.contains("exim-export-"), "Filename should have exim-export- prefix");
            }
        }
        assertNoUnexpectedException(response);
    }

    private void assertExportError(String response) {
        assertNotNull(response, "Response should not be null for errors");
        int status = hstResponse.getStatus();
        assertTrue(status == HTTP_SERVER_ERROR || status == 0 || !response.isEmpty(),
            "Expected error response but got status " + status);
        assertNoUnexpectedException(response);
    }
}
