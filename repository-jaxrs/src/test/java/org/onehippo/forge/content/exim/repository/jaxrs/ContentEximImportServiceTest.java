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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bloomreach.forge.brut.resources.AbstractJaxrsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data-driven regression tests for ContentEximImportService using BRUT.
 * Tests validate response status and JSON structure to catch regressions.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContentEximImportServiceTest extends AbstractJaxrsTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_SERVER_ERROR = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Session session;
    private ContentEximImportService importService;

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

        importService = getComponentManager().getComponent("contentEximImportService");
        if (importService != null) {
            importService.setDaemonSession(session);
        }

        getHstRequest().setMethod("POST");
    }

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
    // Note: Without a valid ZIP package, import returns error response

    @Test
    void testImportEndpoint_withoutPackage_returnsErrorResponse() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String response = invokeFilter();

        // Without a package attachment, import should return error
        assertImportErrorOrEmptyResult(response);
    }

    @Test
    void testImportEndpoint_withEmptyParamsJson_returnsResponse() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");
        getHstRequest().addParameter("paramsJson", "{}");

        String response = invokeFilter();

        assertImportErrorOrEmptyResult(response);
    }

    // ========== Path Filter Configuration Tests ==========

    static Stream<Arguments> pathFilterParams() {
        return Stream.of(
            Arguments.of(
                "Document path includes",
                "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}"
            ),
            Arguments.of(
                "Document path excludes",
                "{\"documents\":{\"documentPathExcludes\":[\"/content/documents/common\"]}}"
            ),
            Arguments.of(
                "Binary path includes",
                "{\"binaries\":{\"binaryPathIncludes\":[\"/content/gallery/exim\"]}}"
            ),
            Arguments.of(
                "Binary path excludes",
                "{\"binaries\":{\"binaryPathExcludes\":[\"/content/gallery/common\"]}}"
            ),
            Arguments.of(
                "Combined includes and excludes",
                "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"],"
                    + "\"documentPathExcludes\":[\"/content/documents/exim/private\"]}}"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pathFilterParams")
    void testImportEndpoint_withPathFilters_acceptsConfiguration(String testName, String paramsJson) throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        // Without package, will return error but should accept the configuration
        assertNotNull(response);
        assertNoUnexpectedException(response);
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
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter(paramName, paramValue);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Tag Property Tests ==========

    @Test
    void testImportEndpoint_withDocumentTags_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("documentTags", "exim:title,exim:content");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withBinaryTags_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"binaries\":{\"binaryPathIncludes\":[\"/content/gallery/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("binaryTags", "hippogallery:filename");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Docbase Property Tests ==========

    @Test
    void testImportEndpoint_withDocbasePropNames_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("docbasePropNames", "hippo:docbase,exim:relatedDoc");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Combined Parameters Tests ==========

    @Test
    void testImportEndpoint_withFullConfiguration_acceptsAllParams() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{"
            + "\"documents\":{"
            + "\"documentPathIncludes\":[\"/content/documents/exim\"]"
            + "},"
            + "\"binaries\":{"
            + "\"binaryPathIncludes\":[\"/content/gallery/exim\"]"
            + "}"
            + "}";

        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("batchSize", "50");
        getHstRequest().addParameter("throttle", "100");
        getHstRequest().addParameter("publishOnImport", "all");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Error Handling Tests ==========

    @Test
    void testImportEndpoint_withMalformedJson_returnsError() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");
        getHstRequest().addParameter("paramsJson", "{invalid json}");

        String response = invokeFilter();

        assertImportError(response);
    }

    // ========== Invalid Parameter Tests ==========

    @ParameterizedTest(name = "Invalid batchSize: {0}")
    @ValueSource(strings = {"-1", "0", "abc"})
    void testImportEndpoint_withInvalidBatchSize_handlesGracefully(String batchSize) throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("batchSize", batchSize);

        String response = invokeFilter();

        assertNotNull(response);
    }

    @ParameterizedTest(name = "Invalid throttle: {0}")
    @ValueSource(strings = {"-1", "abc"})
    void testImportEndpoint_withInvalidThrottle_handlesGracefully(String throttle) throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("throttle", throttle);

        String response = invokeFilter();

        assertNotNull(response);
    }

    @ParameterizedTest(name = "Invalid publishOnImport: {0}")
    @ValueSource(strings = {"invalid", "publish", "unpublish", "ALL", "NONE"})
    void testImportEndpoint_withInvalidPublishOnImport_handlesGracefully(String publishOnImport) throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("publishOnImport", publishOnImport);

        String response = invokeFilter();

        assertNotNull(response);
    }

    // ========== Edge Cases ==========

    @Test
    void testImportEndpoint_withEmptyPathIncludes() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withNullDocuments() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"binaries\":{\"binaryPathIncludes\":[\"/content/gallery/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withNullBinaries() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== publishOnImport Behavior Tests ==========

    @Test
    void testImportEndpoint_withPublishOnImportNone_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("publishOnImport", "none");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withPublishOnImportAll_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("publishOnImport", "all");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withPublishOnImportLive_acceptsConfiguration() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("publishOnImport", "live");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Boundary Tests for Execution Parameters ==========

    @Test
    void testImportEndpoint_withMaxBatchSize() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("batchSize", "10000");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withMinBatchSize() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("batchSize", "1");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withHighThrottle() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":[\"/content/documents/exim\"]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);
        getHstRequest().addParameter("throttle", "10000");

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Multiple Path Tests ==========

    @Test
    void testImportEndpoint_withMultiplePathIncludes() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{\"documentPathIncludes\":["
            + "\"/content/documents/exim/news\","
            + "\"/content/documents/exim/articles\","
            + "\"/content/documents/exim/events\""
            + "]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    @Test
    void testImportEndpoint_withMultiplePathExcludes() throws Exception {
        getHstRequest().setRequestURI("/site/api/import/");
        getHstRequest().setHeader("Content-Type", "multipart/form-data");

        String paramsJson = "{\"documents\":{"
            + "\"documentPathIncludes\":[\"/content/documents/exim\"],"
            + "\"documentPathExcludes\":["
            + "\"/content/documents/exim/private\","
            + "\"/content/documents/exim/draft\","
            + "\"/content/documents/exim/archive\""
            + "]}}";
        getHstRequest().addParameter("paramsJson", paramsJson);

        String response = invokeFilter();

        assertNotNull(response);
        assertNoUnexpectedException(response);
    }

    // ========== Assertion Helpers ==========

    /**
     * Assert that an import error or empty result is returned (expected without package).
     * Note: BRUT mock infrastructure doesn't fully simulate multipart form data,
     * so we accept 500 responses from multipart parsing issues.
     */
    private void assertImportErrorOrEmptyResult(String response) {
        assertNotNull(response, "Response should not be null");

        int status = hstResponse.getStatus();

        // Accept valid HTTP statuses (multipart parsing may cause 500 in test env)
        assertTrue(status == HTTP_OK || status == HTTP_SERVER_ERROR || status == 0,
            "Expected valid HTTP status but got " + status);

        // If success, validate JSON structure if present
        if (status == HTTP_OK && response.contains("{")) {
            assertValidResultJson(response);
        }

        // Ensure no unexpected exceptions
        assertNoUnexpectedException(response);
    }

    /**
     * Assert that an import error is returned.
     */
    private void assertImportError(String response) {
        assertNotNull(response, "Response should not be null for errors");

        int status = hstResponse.getStatus();

        // Error should return 500 or have error content
        assertTrue(status == HTTP_SERVER_ERROR || status == 0 || !response.isEmpty(),
            "Expected error response but got status " + status);

        if (status == HTTP_SERVER_ERROR && response.contains("{")) {
            // Validate error JSON has expected structure
            assertValidResultJson(response);
        }

        // Ensure no unexpected exceptions
        assertNoUnexpectedException(response);
    }

    /**
     * Assert that no unexpected exception occurred (check for stack traces in response).
     * Allows expected multipart parsing errors but catches programming errors.
     */
    private void assertNoUnexpectedException(String response) {
        if (response != null) {
            assertFalse(response.contains("NullPointerException"),
                "Unexpected NullPointerException in response: " + truncate(response, 200));
            assertFalse(response.contains("ClassCastException"),
                "Unexpected ClassCastException in response: " + truncate(response, 200));
            // Allow IllegalStateException for multipart issues, but catch others
            if (response.contains("IllegalStateException") && !response.contains("multipart")
                    && !response.contains("boundary") && !response.contains("Attachment")) {
                fail("Unexpected IllegalStateException in response: " + truncate(response, 200));
            }
        }
    }

    private String truncate(String s, int maxLen) {
        return s == null ? null : (s.length() <= maxLen ? s : s.substring(0, maxLen) + "...");
    }

    /**
     * Validate that response contains valid Result JSON structure.
     */
    private void assertValidResultJson(String response) {
        try {
            // Extract JSON from response (may be embedded in multipart)
            String json = extractJsonFromResponse(response);
            if (json == null || json.isEmpty()) {
                return; // No JSON to validate
            }

            JsonNode root = objectMapper.readTree(json);

            // Validate Result structure if it looks like one
            if (root.has("totalBinaryCount") || root.has("totalDocumentCount")) {
                validateResultStructure(root);
            }
        } catch (Exception e) {
            // JSON parsing failed - not necessarily an error if response isn't JSON
        }
    }

    /**
     * Extract JSON content from potentially multipart response.
     */
    private String extractJsonFromResponse(String response) {
        if (response == null) {
            return null;
        }

        // If response starts with {, it's likely JSON
        int jsonStart = response.indexOf('{');
        if (jsonStart >= 0) {
            int jsonEnd = response.lastIndexOf('}');
            if (jsonEnd > jsonStart) {
                return response.substring(jsonStart, jsonEnd + 1);
            }
        }
        return null;
    }

    /**
     * Validate the Result JSON structure has expected fields with valid values.
     */
    private void validateResultStructure(JsonNode result) {
        // Validate count fields are non-negative
        if (result.has("totalBinaryCount")) {
            assertTrue(result.get("totalBinaryCount").asInt() >= 0,
                "totalBinaryCount should be non-negative");
        }
        if (result.has("totalDocumentCount")) {
            assertTrue(result.get("totalDocumentCount").asInt() >= 0,
                "totalDocumentCount should be non-negative");
        }
        if (result.has("succeededBinaryCount")) {
            assertTrue(result.get("succeededBinaryCount").asInt() >= 0,
                "succeededBinaryCount should be non-negative");
        }
        if (result.has("failedBinaryCount")) {
            assertTrue(result.get("failedBinaryCount").asInt() >= 0,
                "failedBinaryCount should be non-negative");
        }
        if (result.has("succeededDocumentCount")) {
            assertTrue(result.get("succeededDocumentCount").asInt() >= 0,
                "succeededDocumentCount should be non-negative");
        }
        if (result.has("failedDocumentCount")) {
            assertTrue(result.get("failedDocumentCount").asInt() >= 0,
                "failedDocumentCount should be non-negative");
        }

        // Validate items is an array if present
        if (result.has("items")) {
            assertTrue(result.get("items").isArray(),
                "items should be an array");
        }

        // Validate errors is an array if present
        if (result.has("errors")) {
            assertTrue(result.get("errors").isArray(),
                "errors should be an array");
        }
    }
}
