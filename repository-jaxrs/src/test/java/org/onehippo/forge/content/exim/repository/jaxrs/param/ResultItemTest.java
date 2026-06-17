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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultItemTest {

    @Test
    void defaultConstructor_fieldsAreNullOrFalse() {
        ResultItem item = new ResultItem();
        assertNull(item.getPath());
        assertNull(item.getPrimaryType());
        assertFalse(item.isSucceeded());
        assertNull(item.getErrorMessage());
    }

    @Test
    void twoArgConstructor_setsPathAndPrimaryType() {
        ResultItem item = new ResultItem("/content/docs/article", "hippo:document");
        assertEquals("/content/docs/article", item.getPath());
        assertEquals("hippo:document", item.getPrimaryType());
    }

    @Test
    void settersAndGetters_roundtrip() {
        ResultItem item = new ResultItem();
        item.setPath("/content/gallery/images/logo.png");
        item.setPrimaryType("hippogallery:imageset");
        item.setSucceeded(true);
        item.setErrorMessage("none");

        assertEquals("/content/gallery/images/logo.png", item.getPath());
        assertEquals("hippogallery:imageset", item.getPrimaryType());
        assertTrue(item.isSucceeded());
        assertEquals("none", item.getErrorMessage());
    }
}
