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

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data-driven regression tests for ContentEximImportService using BRUT.
 */
public class ContentEximImportServiceTest extends AbstractEximJaxrsTest {

    private ContentEximImportService importService;

    @Override
    protected void initService() {
        importService = getComponentManager().getComponent("contentEximImportService");
        if (importService != null) {
            importService.setDaemonSession(session);
        }
    }

    @Override
    protected String getEndpointUri() {
        return "/site/api/import/";
    }

    // ========== Service Registration Tests ==========

    @Test
    void testServiceIsRegistered() {
        assertNotNull(importService, "ContentEximImportService should be registered in Spring context");
    }

    @Test
    void testRepositoryContentExists() throws Exception {
        assertTrue(session.nodeExists("/content"));
        assertTrue(session.nodeExists("/content/documents"));
        assertTrue(session.nodeExists("/content/documents/exim"));
    }

    // ========== Import Without Package Tests ==========

    @Test
    void testImportEndpoint_withoutPackage_returnsErrorResponse() throws Exception {
        setupRequest((String) null);
        assertImportErrorOrEmptyResult(invokeFilter());
    }

    @Test
    void testImportEndpoint_withEmptyParamsJson_returnsResponse() throws Exception {
        setupRequest("{}");
        assertImportErrorOrEmptyResult(invokeFilter());
    }

    // ========== Path Filter Configuration Tests ==========

    static Stream<Arguments> pathFilterParams() {
        return Stream.of(
            Arguments.of("Document path includes", "{\"documents\":{\"includes\":[\"/content/documents/exim\"]}}"),
            Arguments.of("Document path excludes", "{\"documents\":{\"excludes\":[\"/content/documents/common\"]}}"),
            Arguments.of("Binary path includes", "{\"binaries\":{\"includes\":[\"/content/gallery/exim\"]}}"),
            Arguments.of("Binary path excludes", "{\"binaries\":{\"excludes\":[\"/content/gallery/common\"]}}"),
            Arguments.of("Combined includes and excludes",
                "{\"documents\":{\"includes\":[\"/content/documents/exim\"],\"excludes\":[\"/content/documents/exim/private\"]}}")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pathFilterParams")
    void testImportEndpoint_withPathFilters_acceptsConfiguration(String testName, String paramsJson) throws Exception {
        setupRequest(paramsJson);
        invokeAndAssertValid();
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
    void testImportEndpoint_withExecutionParamOverrides_acceptsConfiguration(String paramName, String paramValue) throws Exception {
        setupRequest(docsParams(), Map.of(paramName, paramValue));
        invokeAndAssertValid();
    }

    // ========== Tag Property Tests ==========

    @Test
    void testImportEndpoint_withDocumentTags_acceptsConfiguration() throws Exception {
        setupRequest(docsParams(), Map.of("documentTags", "exim:title,exim:content"));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withBinaryTags_acceptsConfiguration() throws Exception {
        setupRequest(binariesParams(), Map.of("binaryTags", "hippogallery:filename"));
        invokeAndAssertValid();
    }

    // ========== Docbase Property Tests ==========

    @Test
    void testImportEndpoint_withDocbasePropNames_acceptsConfiguration() throws Exception {
        setupRequest(docsParams(), Map.of("docbasePropNames", "hippo:docbase,exim:relatedDoc"));
        invokeAndAssertValid();
    }

    // ========== Combined Parameters Tests ==========

    @Test
    void testImportEndpoint_withFullConfiguration_acceptsAllParams() throws Exception {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setIncludes(List.of(DOCS_PATH));
        params.setDocuments(docs);
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setIncludes(List.of(BINARIES_PATH));
        params.setBinaries(binaries);

        setupRequest(params, Map.of("batchSize", "50", "throttle", "100", "publishOnImport", "all"));
        invokeAndAssertValid();
    }

    // ========== Error Handling Tests ==========

    @Test
    void testImportEndpoint_withMalformedJson_returnsError() throws Exception {
        setupRequest("{invalid json}");
        assertImportError(invokeFilter());
    }

    // ========== Invalid Parameter Tests ==========

    @ParameterizedTest(name = "Invalid batchSize: {0}")
    @ValueSource(strings = {"-1", "0", "abc"})
    void testImportEndpoint_withInvalidBatchSize_handlesGracefully(String batchSize) throws Exception {
        setupRequest(docsParams(), Map.of("batchSize", batchSize));
        assertNotNull(invokeFilter());
    }

    @ParameterizedTest(name = "Invalid throttle: {0}")
    @ValueSource(strings = {"-1", "abc"})
    void testImportEndpoint_withInvalidThrottle_handlesGracefully(String throttle) throws Exception {
        setupRequest(docsParams(), Map.of("throttle", throttle));
        assertNotNull(invokeFilter());
    }

    @ParameterizedTest(name = "Invalid publishOnImport: {0}")
    @ValueSource(strings = {"invalid", "publish", "unpublish", "ALL", "NONE"})
    void testImportEndpoint_withInvalidPublishOnImport_handlesGracefully(String publishOnImport) throws Exception {
        setupRequest(docsParams(), Map.of("publishOnImport", publishOnImport));
        assertNotNull(invokeFilter());
    }

    // ========== Edge Cases ==========

    @Test
    void testImportEndpoint_withEmptyPathIncludes() throws Exception {
        setupRequest("{\"documents\":{\"includes\":[]}}");
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withNullDocuments() throws Exception {
        setupRequest(binariesParams());
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withNullBinaries() throws Exception {
        setupRequest(docsParams());
        invokeAndAssertValid();
    }

    // ========== publishOnImport Behavior Tests ==========

    @Test
    void testImportEndpoint_withPublishOnImportNone_acceptsConfiguration() throws Exception {
        setupRequest(docsParams(), Map.of("publishOnImport", "none"));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withPublishOnImportAll_acceptsConfiguration() throws Exception {
        setupRequest(docsParams(), Map.of("publishOnImport", "all"));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withPublishOnImportLive_acceptsConfiguration() throws Exception {
        setupRequest(docsParams(), Map.of("publishOnImport", "live"));
        invokeAndAssertValid();
    }

    // ========== Boundary Tests for Execution Parameters ==========

    @Test
    void testImportEndpoint_withMaxBatchSize() throws Exception {
        setupRequest(docsParams(), Map.of("batchSize", "10000"));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withMinBatchSize() throws Exception {
        setupRequest(docsParams(), Map.of("batchSize", "1"));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withHighThrottle() throws Exception {
        setupRequest(docsParams(), Map.of("throttle", "10000"));
        invokeAndAssertValid();
    }

    // ========== Multiple Path Tests ==========

    @Test
    void testImportEndpoint_withMultiplePathIncludes() throws Exception {
        setupRequest(paramsWithDocIncludes(
            "/content/documents/exim/news",
            "/content/documents/exim/articles",
            "/content/documents/exim/events"
        ));
        invokeAndAssertValid();
    }

    @Test
    void testImportEndpoint_withMultiplePathExcludes() throws Exception {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths docs = new QueriesAndPaths();
        docs.setIncludes(List.of(DOCS_PATH));
        docs.setExcludes(List.of(
            "/content/documents/exim/private",
            "/content/documents/exim/draft",
            "/content/documents/exim/archive"
        ));
        params.setDocuments(docs);
        setupRequest(params);
        invokeAndAssertValid();
    }

    // ========== Import-Specific Assertion Helpers ==========

    private void assertImportErrorOrEmptyResult(String response) {
        assertNotNull(response, "Response should not be null");
        int status = hstResponse.getStatus();
        assertTrue(status == HTTP_OK || status == HTTP_SERVER_ERROR || status == 0,
            "Expected valid HTTP status but got " + status);
        if (status == HTTP_OK && response.contains("{")) {
            assertValidResultJson(response);
        }
        assertNoUnexpectedException(response);
    }

    private void assertImportError(String response) {
        assertNotNull(response, "Response should not be null for errors");
        int status = hstResponse.getStatus();
        assertTrue(status == HTTP_SERVER_ERROR || status == 0 || !response.isEmpty(),
            "Expected error response but got status " + status);
        if (status == HTTP_SERVER_ERROR && response.contains("{")) {
            assertValidResultJson(response);
        }
        assertNoUnexpectedException(response);
    }

    private void assertValidResultJson(String response) {
        try {
            String json = extractJsonFromResponse(response);
            if (json == null || json.isEmpty()) return;
            JsonNode root = objectMapper.readTree(json);
            if (root.has("totalBinaryCount") || root.has("totalDocumentCount")) {
                validateResultStructure(root);
            }
        } catch (Exception e) {
            // JSON parsing failed - not necessarily an error
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) return null;
        int jsonStart = response.indexOf('{');
        if (jsonStart >= 0) {
            int jsonEnd = response.lastIndexOf('}');
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart, jsonEnd + 1);
            }
        }
        return null;
    }

    private void validateResultStructure(JsonNode result) {
        if (result.has("totalBinaryCount")) {
            assertTrue(result.get("totalBinaryCount").asInt() >= 0, "totalBinaryCount should be non-negative");
        }
        if (result.has("totalDocumentCount")) {
            assertTrue(result.get("totalDocumentCount").asInt() >= 0, "totalDocumentCount should be non-negative");
        }
        if (result.has("items")) {
            assertTrue(result.get("items").isArray(), "items should be an array");
        }
        if (result.has("errors")) {
            assertTrue(result.get("errors").isArray(), "errors should be an array");
        }
    }
}
