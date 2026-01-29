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
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

class AbstractContentEximServiceTest {

    @TempDir
    Path tempDir;

    private TestableContentEximService service;

    @BeforeEach
    void setUp() {
        service = new TestableContentEximService();
    }

    // ========================================================================
    // isStopRequested tests
    // ========================================================================

    @Test
    void isStopRequested_whenStopFileExists_returnsTrue() throws Exception {
        File baseDir = tempDir.resolve("base").toFile();
        baseDir.mkdirs();
        File eximInf = new File(baseDir, "EXIM-INF");
        eximInf.mkdirs();
        File stopFile = new File(eximInf, "_stop_");
        stopFile.createNewFile();

        FileObject baseFolder = VFS.getManager().resolveFile(baseDir.toURI());

        assertTrue(service.callIsStopRequested(baseFolder));
    }

    @Test
    void isStopRequested_whenStopFileDoesNotExist_returnsFalse() throws Exception {
        File baseDir = tempDir.resolve("base2").toFile();
        baseDir.mkdirs();
        FileObject baseFolder = VFS.getManager().resolveFile(baseDir.toURI());

        assertFalse(service.callIsStopRequested(baseFolder));
    }

    @Test
    void isStopRequested_whenExceptionOccurs_returnsFalse() throws Exception {
        FileObject mockFolder = createMock(FileObject.class);
        expect(mockFolder.resolveFile(anyString())).andThrow(new RuntimeException("Test exception"));
        replay(mockFolder);

        assertFalse(service.callIsStopRequested(mockFolder));
        verify(mockFolder);
    }

    // ========================================================================
    // attachmentToString tests
    // ========================================================================

    @Test
    void attachmentToString_convertsToString() throws Exception {
        String content = "test content";
        Attachment mockAttachment = createMock(Attachment.class);
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        expect(mockAttachment.getObject(InputStream.class)).andReturn(is);
        replay(mockAttachment);

        String result = service.callAttachmentToString(mockAttachment, "UTF-8");

        assertEquals(content, result);
        verify(mockAttachment);
    }

    @Test
    void attachmentToString_handlesUnicodeContent() throws Exception {
        String content = "тест содержимое 中文";
        Attachment mockAttachment = createMock(Attachment.class);
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        expect(mockAttachment.getObject(InputStream.class)).andReturn(is);
        replay(mockAttachment);

        String result = service.callAttachmentToString(mockAttachment, "UTF-8");

        assertEquals(content, result);
        verify(mockAttachment);
    }

    // ========================================================================
    // getAttachmentByContentId tests
    // ========================================================================

    @Test
    void getAttachmentByContentId_withNullList_returnsNull() {
        assertNull(service.callGetAttachmentByContentId(null, "test"));
    }

    @Test
    void getAttachmentByContentId_withEmptyList_returnsNull() {
        assertNull(service.callGetAttachmentByContentId(Collections.emptyList(), "test"));
    }

    @Test
    void getAttachmentByContentId_matchesByContentId() {
        Attachment match = createMock(Attachment.class);
        Attachment other = createMock(Attachment.class);
        expect(match.getContentId()).andReturn("target-id");
        expect(other.getContentId()).andReturn("other-id");
        expect(other.getContentDisposition()).andReturn(null);
        replay(match, other);

        Attachment result = service.callGetAttachmentByContentId(Arrays.asList(other, match), "target-id");

        assertSame(match, result);
        verify(match, other);
    }

    @Test
    void getAttachmentByContentId_matchesByContentDispositionName() {
        Attachment match = createMock(Attachment.class);
        ContentDisposition disposition = createMock(ContentDisposition.class);
        expect(match.getContentId()).andReturn("wrong-id");
        expect(match.getContentDisposition()).andReturn(disposition);
        expect(disposition.getParameter("name")).andReturn("target-name");
        replay(match, disposition);

        Attachment result = service.callGetAttachmentByContentId(Collections.singletonList(match), "target-name");

        assertSame(match, result);
        verify(match, disposition);
    }

    @Test
    void getAttachmentByContentId_noMatch_returnsNull() {
        Attachment attachment = createMock(Attachment.class);
        expect(attachment.getContentId()).andReturn("other-id");
        expect(attachment.getContentDisposition()).andReturn(null);
        replay(attachment);

        Attachment result = service.callGetAttachmentByContentId(Collections.singletonList(attachment), "target-id");

        assertNull(result);
        verify(attachment);
    }

    // ========================================================================
    // applyTagContentProperties tests
    // ========================================================================

    @Test
    void applyTagContentProperties_withNullTags_returnsFalse() {
        ContentNode contentNode = new ContentNode();
        assertFalse(service.callApplyTagContentProperties(contentNode, null));
    }

    @Test
    void applyTagContentProperties_withEmptyTags_returnsFalse() {
        ContentNode contentNode = new ContentNode();
        assertFalse(service.callApplyTagContentProperties(contentNode, Collections.emptySet()));
    }

    @Test
    void applyTagContentProperties_appliesValidTags() {
        ContentNode contentNode = new ContentNode();
        Set<String> tags = new LinkedHashSet<>();
        tags.add("myproject:tags=a,b,c");

        boolean result = service.callApplyTagContentProperties(contentNode, tags);

        assertTrue(result);
    }

    @Test
    void applyTagContentProperties_skipsInvalidTagsWithoutEquals() {
        ContentNode contentNode = new ContentNode();
        Set<String> tags = new LinkedHashSet<>();
        tags.add("invalidtag");

        boolean result = service.callApplyTagContentProperties(contentNode, tags);

        assertFalse(result);
    }

    @Test
    void applyTagContentProperties_skipsTagsWithBlankName() {
        ContentNode contentNode = new ContentNode();
        Set<String> tags = new LinkedHashSet<>();
        tags.add("=value");

        boolean result = service.callApplyTagContentProperties(contentNode, tags);

        assertFalse(result);
    }

    @Test
    void applyTagContentProperties_skipsTagsWithBlankValue() {
        ContentNode contentNode = new ContentNode();
        Set<String> tags = new LinkedHashSet<>();
        tags.add("name=");

        boolean result = service.callApplyTagContentProperties(contentNode, tags);

        assertFalse(result);
    }

    // ========================================================================
    // getUserPrincipalName tests
    // ========================================================================

    @Test
    void getUserPrincipalName_fromSecurityContext() {
        SecurityContext securityContext = createMock(SecurityContext.class);
        Principal principal = createMock(Principal.class);
        expect(securityContext.getUserPrincipal()).andReturn(principal);
        expect(principal.getName()).andReturn("testuser");
        replay(securityContext, principal);

        String result = service.callGetUserPrincipalName(securityContext, null);

        assertEquals("testuser", result);
        verify(securityContext, principal);
    }

    @Test
    void getUserPrincipalName_fromRequest_whenSecurityContextHasNoPrincipal() {
        SecurityContext securityContext = createMock(SecurityContext.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        Principal principal = createMock(Principal.class);
        expect(securityContext.getUserPrincipal()).andReturn(null);
        expect(request.getUserPrincipal()).andReturn(principal);
        expect(principal.getName()).andReturn("requestuser");
        replay(securityContext, request, principal);

        String result = service.callGetUserPrincipalName(securityContext, request);

        assertEquals("requestuser", result);
        verify(securityContext, request, principal);
    }

    @Test
    void getUserPrincipalName_fromBasicAuth_whenNoPrincipal() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getUserPrincipal()).andReturn(null);
        String credentials = Base64.getEncoder().encodeToString("basicuser:password".getBytes());
        expect(request.getHeader("Authorization")).andReturn("Basic " + credentials);
        replay(request);

        String result = service.callGetUserPrincipalName(null, request);

        assertEquals("basicuser", result);
        verify(request);
    }

    @Test
    void getUserPrincipalName_returnsNull_whenNoAuthInfo() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        expect(request.getUserPrincipal()).andReturn(null);
        expect(request.getHeader("Authorization")).andReturn(null);
        replay(request);

        String result = service.callGetUserPrincipalName(null, request);

        assertNull(result);
        verify(request);
    }

    @Test
    void getUserPrincipalName_returnsNull_whenBothContextAndRequestNull() {
        assertNull(service.callGetUserPrincipalName(null, null));
    }

    // ========================================================================
    // overrideExecutionParamsByParameters tests
    // ========================================================================

    @Test
    void overrideExecutionParams_overridesBatchSize() {
        ExecutionParams params = new ExecutionParams();
        int originalBatchSize = params.getBatchSize();

        service.callOverrideExecutionParams(params, "50", null, null, null, null, null, null);

        assertEquals(50, (int) params.getBatchSize());
        assertNotEquals((long) originalBatchSize, (long) params.getBatchSize());
    }

    @Test
    void overrideExecutionParams_overridesThrottle() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, "100", null, null, null, null, null);

        assertEquals(100L, (long) params.getThrottle());
    }

    @Test
    void overrideExecutionParams_overridesPublishOnImport() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, null, "none", null, null, null, null);

        assertEquals("none", params.getPublishOnImport());
    }

    @Test
    void overrideExecutionParams_overridesDataUrlSizeThreshold() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, null, null, "1024", null, null, null);

        assertEquals(1024L, (long) params.getDataUrlSizeThreshold());
    }

    @Test
    void overrideExecutionParams_overridesDocbasePropNames() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, null, null, null, "prop1,prop2", null, null);

        Set<String> expected = new LinkedHashSet<>(Arrays.asList("prop1", "prop2"));
        assertEquals(expected, params.getDocbasePropNames());
    }

    @Test
    void overrideExecutionParams_overridesDocumentTags() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, null, null, null, null, "tag1=a;tag2=b", null);

        Set<String> expected = new LinkedHashSet<>(Arrays.asList("tag1=a", "tag2=b"));
        assertEquals(expected, params.getDocumentTags());
    }

    @Test
    void overrideExecutionParams_overridesBinaryTags() {
        ExecutionParams params = new ExecutionParams();

        service.callOverrideExecutionParams(params, null, null, null, null, null, null, "btag1=x;btag2=y");

        Set<String> expected = new LinkedHashSet<>(Arrays.asList("btag1=x", "btag2=y"));
        assertEquals(expected, params.getBinaryTags());
    }

    @Test
    void overrideExecutionParams_ignoresBlankValues() {
        ExecutionParams params = new ExecutionParams();
        int originalBatchSize = params.getBatchSize();

        service.callOverrideExecutionParams(params, "", "  ", null, "", null, null, null);

        assertEquals((long) originalBatchSize, (long) params.getBatchSize());
    }

    // ========================================================================
    // isBinaryPathIncluded / isDocumentPathIncluded tests
    // ========================================================================

    @Test
    void isBinaryPathIncluded_withNullQueriesAndPaths_returnsTrue() {
        ExecutionParams params = new ExecutionParams();
        params.setBinaries(null);

        assertTrue(service.callIsBinaryPathIncluded(params, "/content/gallery/test.jpg"));
    }

    @Test
    void isBinaryPathIncluded_excludedPath_returnsFalse() {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setExcludes(Collections.singletonList("/content/gallery/excluded/**"));
        params.setBinaries(binaries);

        assertFalse(service.callIsBinaryPathIncluded(params, "/content/gallery/excluded/test.jpg"));
    }

    @Test
    void isBinaryPathIncluded_includedPath_returnsTrue() {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setIncludes(Collections.singletonList("/content/gallery/included/**"));
        params.setBinaries(binaries);

        assertTrue(service.callIsBinaryPathIncluded(params, "/content/gallery/included/test.jpg"));
    }

    @Test
    void isBinaryPathIncluded_notInIncludes_returnsFalse() {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths binaries = new QueriesAndPaths();
        binaries.setIncludes(Collections.singletonList("/content/gallery/included/**"));
        params.setBinaries(binaries);

        assertFalse(service.callIsBinaryPathIncluded(params, "/content/gallery/other/test.jpg"));
    }

    @Test
    void isDocumentPathIncluded_withNullQueriesAndPaths_returnsTrue() {
        ExecutionParams params = new ExecutionParams();
        params.setDocuments(null);

        assertTrue(service.callIsDocumentPathIncluded(params, "/content/documents/test"));
    }

    @Test
    void isDocumentPathIncluded_excludedPath_returnsFalse() {
        ExecutionParams params = new ExecutionParams();
        QueriesAndPaths documents = new QueriesAndPaths();
        documents.setExcludes(Collections.singletonList("/content/documents/excluded/**"));
        params.setDocuments(documents);

        assertFalse(service.callIsDocumentPathIncluded(params, "/content/documents/excluded/test"));
    }

    // ========================================================================
    // recordToResultItem tests
    // ========================================================================

    @Test
    void recordToResultItem_convertsRecord() {
        ContentMigrationRecord record = new ContentMigrationRecord();
        record.setContentPath("/test/path");
        record.setContentType("myproject:document");
        record.setSucceeded(true);
        record.setErrorMessage(null);

        ResultItem result = service.callRecordToResultItem(record);

        assertEquals("/test/path", result.getPath());
        assertEquals("myproject:document", result.getPrimaryType());
        assertTrue(result.isSucceeded());
        assertNull(result.getErrorMessage());
    }

    @Test
    void recordToResultItem_preservesErrorMessage() {
        ContentMigrationRecord record = new ContentMigrationRecord();
        record.setContentPath("/test/path");
        record.setContentType("myproject:document");
        record.setSucceeded(false);
        record.setErrorMessage("Test error");

        ResultItem result = service.callRecordToResultItem(record);

        assertFalse(result.isSucceeded());
        assertEquals("Test error", result.getErrorMessage());
    }

    // ========================================================================
    // toJsonString tests
    // ========================================================================

    @Test
    void toJsonString_serializesObject() throws Exception {
        ExecutionParams params = new ExecutionParams();
        params.setBatchSize(100);

        String json = service.callToJsonString(params);

        assertNotNull(json);
        assertTrue(json.contains("\"batchSize\""));
        assertTrue(json.contains("100"));
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableContentEximService extends AbstractContentEximService {

        boolean callIsStopRequested(FileObject baseFolder) {
            return isStopRequested(baseFolder);
        }

        String callAttachmentToString(Attachment attachment, String charset) throws Exception {
            return attachmentToString(attachment, charset);
        }

        Attachment callGetAttachmentByContentId(List<Attachment> attachments, String contentId) {
            return getAttachmentByContentId(attachments, contentId);
        }

        boolean callApplyTagContentProperties(ContentNode contentNode, Set<String> tagInfos) {
            return applyTagContentProperties(contentNode, tagInfos);
        }

        String callGetUserPrincipalName(SecurityContext securityContext, HttpServletRequest request) {
            return getUserPrincipalName(securityContext, request);
        }

        void callOverrideExecutionParams(ExecutionParams params, String batchSize, String throttle,
                String publishOnImport, String dataUrlSizeThreshold, String docbasePropNames,
                String documentTags, String binaryTags) {
            overrideExecutionParamsByParameters(params, batchSize, throttle, publishOnImport,
                    dataUrlSizeThreshold, docbasePropNames, documentTags, binaryTags);
        }

        boolean callIsBinaryPathIncluded(ExecutionParams params, String path) {
            return isBinaryPathIncluded(new org.onehippo.forge.content.exim.core.util.AntPathMatcher(), params, path);
        }

        boolean callIsDocumentPathIncluded(ExecutionParams params, String path) {
            return isDocumentPathIncluded(new org.onehippo.forge.content.exim.core.util.AntPathMatcher(), params, path);
        }

        ResultItem callRecordToResultItem(ContentMigrationRecord record) {
            return recordToResultItem(record);
        }

        String callToJsonString(Object object) throws Exception {
            return toJsonString(object);
        }
    }
}
