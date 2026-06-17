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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueriesAndPathsTest {

    private QueriesAndPaths qap;

    @BeforeEach
    void setUp() {
        qap = new QueriesAndPaths();
    }

    @Test
    void defaultState_allListsAreEmpty() {
        assertTrue(qap.getQueries().isEmpty());
        assertTrue(qap.getPaths().isEmpty());
        assertTrue(qap.getIncludes().isEmpty());
        assertTrue(qap.getExcludes().isEmpty());
    }

    @Test
    void setQueries_andGet_returnsUnmodifiableView() {
        qap.setQueries(List.of("//element(*,hippo:document)"));
        assertEquals(1, qap.getQueries().size());
        assertEquals("//element(*,hippo:document)", qap.getQueries().get(0));
        assertThrows(UnsupportedOperationException.class, () -> qap.getQueries().add("more"));
    }

    @Test
    void setPaths_andGet_returnsUnmodifiableView() {
        qap.setPaths(List.of("/content/documents/myproject", "/content/gallery"));
        assertEquals(2, qap.getPaths().size());
        assertThrows(UnsupportedOperationException.class, () -> qap.getPaths().clear());
    }

    @Test
    void setIncludes_andGet_roundtrips() {
        qap.setIncludes(List.of("/content/documents/**"));
        assertEquals(1, qap.getIncludes().size());
    }

    @Test
    void setExcludes_andGet_roundtrips() {
        qap.setExcludes(List.of("/content/documents/archive/**"));
        assertEquals(1, qap.getExcludes().size());
    }

    @Test
    void setQueries_null_getQueriesReturnsEmpty() {
        qap.setQueries(List.of("q1"));
        qap.setQueries(null);
        assertTrue(qap.getQueries().isEmpty());
    }
}
