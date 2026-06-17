/*
 * Copyright 2025 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.core;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentMigrationRecordTest {

    private ContentMigrationRecord record;

    @BeforeEach
    void setUp() {
        record = new ContentMigrationRecord();
    }

    @Test
    void defaultState_allFieldsAreBlankOrFalse() {
        assertFalse(record.isProcessed());
        assertFalse(record.isSucceeded());
        assertNull(record.getErrorMessage());
        assertNull(record.getContentType());
        assertNull(record.getContentId());
        assertNull(record.getContentPath());
        assertTrue(record.getAttributeMap().isEmpty());
        assertTrue(record.getAttributeNames().isEmpty());
    }

    @Test
    void setProcessed_andGet_roundtrips() {
        record.setProcessed(true);
        assertTrue(record.isProcessed());
        record.setProcessed(false);
        assertFalse(record.isProcessed());
    }

    @Test
    void setSucceeded_andGet_roundtrips() {
        record.setSucceeded(true);
        assertTrue(record.isSucceeded());
    }

    @Test
    void setErrorMessage_andGet_roundtrips() {
        record.setErrorMessage("something went wrong");
        assertEquals("something went wrong", record.getErrorMessage());
    }

    @Test
    void setContentFields_andGet_roundtrips() {
        record.setContentType("hippo:document");
        record.setContentId("abc-123");
        record.setContentPath("/content/documents/myproject/news/article");

        assertEquals("hippo:document", record.getContentType());
        assertEquals("abc-123", record.getContentId());
        assertEquals("/content/documents/myproject/news/article", record.getContentPath());
    }

    @Test
    void setAttribute_andGet_returnsStoredValue() {
        record.setAttribute("myKey", "myValue");
        assertEquals("myValue", record.getAttribute("myKey"));
    }

    @Test
    void getAttribute_missingKey_returnsNull() {
        assertNull(record.getAttribute("nonexistent"));
    }

    @Test
    void getAttributeAsString_existingKey_returnsStringValue() {
        record.setAttribute("count", 42);
        assertEquals("42", record.getAttributeAsString("count"));
    }

    @Test
    void getAttributeAsString_missingKey_returnsEmptyString() {
        assertEquals("", record.getAttributeAsString("missing"));
    }

    @Test
    void getAttributeAsAtomicInteger_createTrue_createsAndReturnsCounter() {
        AtomicInteger counter = record.getAttributeAsAtomicInteger("hits", true);
        assertNotNull(counter);
        assertEquals(0, counter.get());
        counter.incrementAndGet();
        assertEquals(1, record.getAttributeAsAtomicInteger("hits", false).get());
    }

    @Test
    void getAttributeAsAtomicInteger_createFalse_whenMissing_returnsNull() {
        assertNull(record.getAttributeAsAtomicInteger("nonexistent", false));
    }

    @Test
    void getAttributeAsCollection_createTrue_createsAndReturnsCollection() {
        Collection<Object> col = record.getAttributeAsCollection("items", true);
        assertNotNull(col);
        col.add("element1");
        Collection<Object> retrieved = record.getAttributeAsCollection("items", false);
        assertEquals(1, retrieved.size());
        assertTrue(retrieved.contains("element1"));
    }

    @Test
    void getAttributeAsCollection_createFalse_whenMissing_returnsNull() {
        assertNull(record.getAttributeAsCollection("nonexistent", false));
    }

    @Test
    void removeAttribute_existingKey_returnsRemovedValue() {
        record.setAttribute("toRemove", "value");
        Object removed = record.removeAttribute("toRemove");
        assertEquals("value", removed);
        assertNull(record.getAttribute("toRemove"));
    }

    @Test
    void removeAttribute_noAttributesMap_returnsNull() {
        // attributes map is never initialised
        assertNull(record.removeAttribute("anything"));
    }

    @Test
    void getAttributeMap_returnsUnmodifiableView() {
        record.setAttribute("k", "v");
        assertTrue(record.getAttributeMap().containsKey("k"));
    }

    @Test
    void getAttributeNames_afterSet_containsKey() {
        record.setAttribute("x", 1);
        assertTrue(record.getAttributeNames().contains("x"));
    }

    @Test
    void toString_containsCoreFields() {
        record.setContentId("id-1");
        record.setContentPath("/content/docs/article");
        record.setSucceeded(true);
        String str = record.toString();
        assertTrue(str.contains("id-1"));
        assertTrue(str.contains("/content/docs/article"));
        assertTrue(str.contains("succeeded=true"));
    }
}
