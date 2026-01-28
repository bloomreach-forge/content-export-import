/*
 * Copyright 2016-2024 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.core.impl;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onehippo.forge.content.exim.core.DocumentManagerException;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;
import org.onehippo.forge.content.pojo.model.ContentNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowDocumentManagerImplTest {

    private Session mockSession;
    private WorkflowDocumentManagerImpl documentManager;

    @BeforeEach
    void setUp() {
        mockSession = EasyMock.createMock(Session.class);
        documentManager = new WorkflowDocumentManagerImpl(mockSession);
    }

    @Test
    void getContentNodeBinder_whenNull_createsDefaultBinder() {
        ContentNodeBinder<Node, ContentItem, Value> binder = documentManager.getContentNodeBinder();

        assertNotNull(binder);
        assertInstanceOf(DefaultJcrContentNodeBinder.class, binder);
    }

    @Test
    void getContentNodeBinder_whenCalledTwice_returnsSameInstance() {
        ContentNodeBinder<Node, ContentItem, Value> first = documentManager.getContentNodeBinder();
        ContentNodeBinder<Node, ContentItem, Value> second = documentManager.getContentNodeBinder();

        assertSame(first, second);
    }

    @Test
    void setContentNodeBinder_overridesDefaultBinder() {
        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> customBinder = EasyMock.createMock(ContentNodeBinder.class);

        documentManager.setContentNodeBinder(customBinder);

        assertSame(customBinder, documentManager.getContentNodeBinder());
    }

    @Test
    void setContentNodeBinder_afterLazyInit_replacesExistingBinder() {
        ContentNodeBinder<Node, ContentItem, Value> defaultBinder = documentManager.getContentNodeBinder();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> customBinder = EasyMock.createMock(ContentNodeBinder.class);
        documentManager.setContentNodeBinder(customBinder);

        assertNotSame(defaultBinder, documentManager.getContentNodeBinder());
        assertSame(customBinder, documentManager.getContentNodeBinder());
    }

    @Test
    void getContentNodeBindingItemFilter_whenNull_createsDefaultFilterWithExcludes() {
        ContentNodeBindingItemFilter<ContentItem> filter = documentManager.getContentNodeBindingItemFilter();

        assertNotNull(filter);
        assertInstanceOf(DefaultContentNodeJcrBindingItemFilter.class, filter);
    }

    @Test
    void getContentNodeBindingItemFilter_whenCalledTwice_returnsSameInstance() {
        ContentNodeBindingItemFilter<ContentItem> first = documentManager.getContentNodeBindingItemFilter();
        ContentNodeBindingItemFilter<ContentItem> second = documentManager.getContentNodeBindingItemFilter();

        assertSame(first, second);
    }

    @Test
    void setContentNodeBindingItemFilter_overridesDefaultFilter() {
        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> customFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);

        documentManager.setContentNodeBindingItemFilter(customFilter);

        assertSame(customFilter, documentManager.getContentNodeBindingItemFilter());
    }

    @Test
    void setContentNodeBindingItemFilter_afterLazyInit_replacesExistingFilter() {
        ContentNodeBindingItemFilter<ContentItem> defaultFilter = documentManager.getContentNodeBindingItemFilter();

        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> customFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);
        documentManager.setContentNodeBindingItemFilter(customFilter);

        assertNotSame(defaultFilter, documentManager.getContentNodeBindingItemFilter());
        assertSame(customFilter, documentManager.getContentNodeBindingItemFilter());
    }

    @Test
    void updateEditableDocument_bindsContentNodeToDocumentNode() throws Exception {
        Document mockDocument = EasyMock.createMock(Document.class);
        Node mockNode = EasyMock.createMock(Node.class);
        ContentNode sourceContentNode = new ContentNode();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> mockBinder = EasyMock.createMock(ContentNodeBinder.class);

        expect(mockDocument.getNode(mockSession)).andReturn(mockNode);
        mockBinder.bind(EasyMock.eq(mockNode), EasyMock.eq(sourceContentNode), EasyMock.anyObject());
        EasyMock.expectLastCall();

        replay(mockDocument, mockNode, mockBinder);

        documentManager.setContentNodeBinder(mockBinder);
        documentManager.updateEditableDocument(mockDocument, sourceContentNode);

        verify(mockDocument, mockNode, mockBinder);
    }

    @Test
    void updateEditableDocument_whenBindFails_throwsDocumentManagerException() throws Exception {
        Document mockDocument = EasyMock.createMock(Document.class);
        Node mockNode = EasyMock.createMock(Node.class);
        ContentNode sourceContentNode = new ContentNode();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> mockBinder = EasyMock.createMock(ContentNodeBinder.class);

        expect(mockDocument.getNode(mockSession)).andReturn(mockNode);
        mockBinder.bind(EasyMock.eq(mockNode), EasyMock.eq(sourceContentNode), EasyMock.anyObject());
        EasyMock.expectLastCall().andThrow(new RuntimeException("Binding failed"));

        replay(mockDocument, mockNode, mockBinder);

        documentManager.setContentNodeBinder(mockBinder);

        assertThrows(DocumentManagerException.class, () -> {
            documentManager.updateEditableDocument(mockDocument, sourceContentNode);
        });
    }

    @Test
    void updateEditableDocument_usesConfiguredFilter() throws Exception {
        Document mockDocument = EasyMock.createMock(Document.class);
        Node mockNode = EasyMock.createMock(Node.class);
        ContentNode sourceContentNode = new ContentNode();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> mockBinder = EasyMock.createMock(ContentNodeBinder.class);
        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> mockFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);

        expect(mockDocument.getNode(mockSession)).andReturn(mockNode);
        mockBinder.bind(EasyMock.eq(mockNode), EasyMock.eq(sourceContentNode), EasyMock.eq(mockFilter));
        EasyMock.expectLastCall();

        replay(mockDocument, mockNode, mockBinder, mockFilter);

        documentManager.setContentNodeBinder(mockBinder);
        documentManager.setContentNodeBindingItemFilter(mockFilter);
        documentManager.updateEditableDocument(mockDocument, sourceContentNode);

        verify(mockDocument, mockNode, mockBinder);
    }
}
