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
package org.onehippo.forge.content.exim.repository.jaxrs.param;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    private Result result;

    @BeforeEach
    void setUp() {
        result = new Result();
    }

    @Test
    void defaultState_allCountersAreZero() {
        assertEquals(0, result.getTotalBinaryCount());
        assertEquals(0, result.getTotalDocumentCount());
        assertEquals(0, result.getSucceededBinaryCount());
        assertEquals(0, result.getFailedBinaryCount());
        assertEquals(0, result.getSucceededDocumentCount());
        assertEquals(0, result.getFailedDocumentCount());
    }

    @Test
    void defaultState_itemsAndErrorsAreEmpty() {
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void incrementTotalBinaryCount_incrementsBy1() {
        int val = result.incrementTotalBinaryCount();
        assertEquals(1, val);
        assertEquals(1, result.getTotalBinaryCount());
    }

    @Test
    void incrementTotalDocumentCount_incrementsBy1() {
        result.incrementTotalDocumentCount();
        assertEquals(1, result.getTotalDocumentCount());
    }

    @Test
    void incrementSucceededBinaryCount_incrementsBy1() {
        result.incrementSucceededBinaryCount();
        assertEquals(1, result.getSucceededBinaryCount());
    }

    @Test
    void incrementFailedBinaryCount_incrementsBy1() {
        result.incrementFailedBinaryCount();
        assertEquals(1, result.getFailedBinaryCount());
    }

    @Test
    void incrementSucceededDocumentCount_incrementsBy1() {
        result.incrementSucceededDocumentCount();
        assertEquals(1, result.getSucceededDocumentCount());
    }

    @Test
    void incrementFailedDocumentCount_incrementsBy1() {
        result.incrementFailedDocumentCount();
        assertEquals(1, result.getFailedDocumentCount());
    }

    @Test
    void addItem_addsToItemList() {
        ResultItem item = new ResultItem("/content/docs/article", "hippo:document");
        result.addItem(item);
        List<ResultItem> items = result.getItems();
        assertEquals(1, items.size());
        assertEquals("/content/docs/article", items.get(0).getPath());
    }

    @Test
    void setItems_replacesItemList() {
        result.addItem(new ResultItem("/old", "type"));
        result.setItems(List.of(new ResultItem("/new", "type")));
        assertEquals(1, result.getItems().size());
        assertEquals("/new", result.getItems().get(0).getPath());
    }

    @Test
    void setItems_null_clearsItems() {
        result.addItem(new ResultItem("/path", "type"));
        result.setItems(null);
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void addError_addsErrorMessage() {
        result.addError("something failed");
        assertEquals(1, result.getErrors().size());
        assertEquals("something failed", result.getErrors().get(0));
    }

    @Test
    void setErrors_null_clearsErrors() {
        result.addError("err");
        result.setErrors(null);
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void getProgress_whenNoCounts_returnsNaN() {
        assertTrue(Double.isNaN(result.getProgress()));
    }

    @Test
    void getProgress_afterProcessing_returnsCorrectRatio() {
        result.setTotalBinaryCount(2);
        result.setTotalDocumentCount(2);
        result.incrementSucceededBinaryCount();
        result.incrementSucceededDocumentCount();
        // 2 processed out of 4 total = 0.5
        assertEquals(0.5, result.getProgress(), 0.001);
    }

    @Test
    void setTotalCounts_andGet_roundtrips() {
        result.setTotalBinaryCount(10);
        result.setTotalDocumentCount(20);
        assertEquals(10, result.getTotalBinaryCount());
        assertEquals(20, result.getTotalDocumentCount());
    }
}
