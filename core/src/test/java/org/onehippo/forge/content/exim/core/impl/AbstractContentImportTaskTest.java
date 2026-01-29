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

import static org.junit.jupiter.api.Assertions.*;

class AbstractContentImportTaskTest {

    private DocumentManager mockDocumentManager;
    private AbstractContentImportTask task;

    @BeforeEach
    void setUp() {
        mockDocumentManager = EasyMock.createMock(DocumentManager.class);
        task = new AbstractContentImportTask(mockDocumentManager) {};
    }

    @Test
    void getContentNodeBinder_whenNull_createsDefaultBinder() {
        ContentNodeBinder<Node, ContentItem, Value> binder = task.getContentNodeBinder();

        assertNotNull(binder);
        assertInstanceOf(DefaultJcrContentNodeBinder.class, binder);
    }

    @Test
    void getContentNodeBinder_whenCalledTwice_returnsSameInstance() {
        ContentNodeBinder<Node, ContentItem, Value> first = task.getContentNodeBinder();
        ContentNodeBinder<Node, ContentItem, Value> second = task.getContentNodeBinder();

        assertSame(first, second);
    }

    @Test
    void setContentNodeBinder_overridesDefaultBinder() {
        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> customBinder = EasyMock.createMock(ContentNodeBinder.class);

        task.setContentNodeBinder(customBinder);

        assertSame(customBinder, task.getContentNodeBinder());
    }

    @Test
    void setContentNodeBinder_afterLazyInit_replacesExistingBinder() {
        ContentNodeBinder<Node, ContentItem, Value> defaultBinder = task.getContentNodeBinder();

        @SuppressWarnings("unchecked")
        ContentNodeBinder<Node, ContentItem, Value> customBinder = EasyMock.createMock(ContentNodeBinder.class);
        task.setContentNodeBinder(customBinder);

        assertNotSame(defaultBinder, task.getContentNodeBinder());
        assertSame(customBinder, task.getContentNodeBinder());
    }

    @Test
    void getContentNodeBindingItemFilter_whenNull_createsDefaultFilter() {
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
}
