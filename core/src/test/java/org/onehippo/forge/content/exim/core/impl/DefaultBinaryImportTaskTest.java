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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onehippo.forge.content.exim.core.DocumentManager;
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

class DefaultBinaryImportTaskTest {

    private DocumentManager mockDocumentManager;
    private Session mockSession;
    private DefaultBinaryImportTask task;

    @BeforeEach
    void setUp() {
        mockDocumentManager = EasyMock.createMock(DocumentManager.class);
        mockSession = EasyMock.createMock(Session.class);
        expect(mockDocumentManager.getSession()).andReturn(mockSession).anyTimes();
        replay(mockDocumentManager);
        task = new DefaultBinaryImportTask(mockDocumentManager);
    }

    @Test
    void getContentNodeBindingItemFilter_whenNull_createsFilterWithBinaryExcludes() {
        ContentNodeBindingItemFilter<ContentItem> filter = task.getContentNodeBindingItemFilter();

        assertNotNull(filter);
        assertInstanceOf(DefaultContentNodeJcrBindingItemFilter.class, filter);
    }

    @Test
    void getContentNodeBindingItemFilter_whenCalledTwice_returnsSameInstance() {
        ContentNodeBindingItemFilter<ContentItem> first = task.getContentNodeBindingItemFilter();
        ContentNodeBindingItemFilter<ContentItem> second = task.getContentNodeBindingItemFilter();

        assertSame(first, second);
    }

    @Test
    void setContentNodeBindingItemFilter_overridesDefaultFilter() {
        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> customFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);

        task.setContentNodeBindingItemFilter(customFilter);

        assertSame(customFilter, task.getContentNodeBindingItemFilter());
    }

    @Test
    void setContentNodeBindingItemFilter_afterLazyInit_replacesExistingFilter() {
        ContentNodeBindingItemFilter<ContentItem> defaultFilter = task.getContentNodeBindingItemFilter();

        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> customFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);
        task.setContentNodeBindingItemFilter(customFilter);

        assertNotSame(defaultFilter, task.getContentNodeBindingItemFilter());
        assertSame(customFilter, task.getContentNodeBindingItemFilter());
    }

    @Test
    void getContentNodeBinder_inheritedFromAbstract_returnsDefaultBinder() {
        ContentNodeBinder<Node, ContentItem, Value> binder = task.getContentNodeBinder();

        assertNotNull(binder);
        assertInstanceOf(DefaultJcrContentNodeBinder.class, binder);
    }

    @Test
    void setContentNodeBinder_overridesInheritedBinder() {
        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> customBinder = EasyMock.createMock(ContentNodeBinder.class);

        task.setContentNodeBinder(customBinder);

        assertSame(customBinder, task.getContentNodeBinder());
    }

    @Test
    void updateBinaryHandleAndVariantNode_usesBinderToBindContent() throws Exception {
        Node mockHandleNode = EasyMock.createMock(Node.class);
        Node mockVariantNode = EasyMock.createMock(Node.class);
        ContentNode contentNode = new ContentNode();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> mockBinder = EasyMock.createMock(ContentNodeBinder.class);

        expect(mockHandleNode.getName()).andReturn("testBinary");
        expect(mockHandleNode.getNode("testBinary")).andReturn(mockVariantNode);
        mockBinder.bind(EasyMock.eq(mockVariantNode), EasyMock.eq(contentNode),
                EasyMock.anyObject(ContentNodeBindingItemFilter.class), EasyMock.anyObject());
        EasyMock.expectLastCall();

        replay(mockHandleNode, mockVariantNode, mockBinder);

        task.setContentNodeBinder(mockBinder);
        task.updateBinaryHandleAndVariantNodeFromBinaryVariantContentNode(mockHandleNode, contentNode);

        verify(mockHandleNode, mockVariantNode, mockBinder);
    }

    @Test
    void updateBinaryHandleAndVariantNode_usesConfiguredFilterAndConverter() throws Exception {
        Node mockHandleNode = EasyMock.createMock(Node.class);
        Node mockVariantNode = EasyMock.createMock(Node.class);
        ContentNode contentNode = new ContentNode();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> mockBinder = EasyMock.createMock(ContentNodeBinder.class);
        @SuppressWarnings("unchecked")
        ContentNodeBindingItemFilter<ContentItem> mockFilter = EasyMock.createMock(ContentNodeBindingItemFilter.class);

        expect(mockHandleNode.getName()).andReturn("testBinary");
        expect(mockHandleNode.getNode("testBinary")).andReturn(mockVariantNode);
        mockBinder.bind(EasyMock.eq(mockVariantNode), EasyMock.eq(contentNode),
                EasyMock.eq(mockFilter), EasyMock.anyObject());
        EasyMock.expectLastCall();

        replay(mockHandleNode, mockVariantNode, mockBinder, mockFilter);

        task.setContentNodeBinder(mockBinder);
        task.setContentNodeBindingItemFilter(mockFilter);
        task.updateBinaryHandleAndVariantNodeFromBinaryVariantContentNode(mockHandleNode, contentNode);

        verify(mockHandleNode, mockVariantNode, mockBinder);
    }
}
