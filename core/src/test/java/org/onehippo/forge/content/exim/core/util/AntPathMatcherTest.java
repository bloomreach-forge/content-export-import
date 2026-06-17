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
package org.onehippo.forge.content.exim.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntPathMatcherTest {

    private AntPathMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new AntPathMatcher();
    }

    @Test
    void isPattern_withWildcard_returnsTrue() {
        assertTrue(matcher.isPattern("/content/**"));
        assertTrue(matcher.isPattern("/content/?/docs"));
    }

    @Test
    void isPattern_withoutWildcard_returnsFalse() {
        assertFalse(matcher.isPattern("/content/documents/myproject"));
    }

    @Test
    void match_exactPath_returnsTrue() {
        assertTrue(matcher.match("/content/documents/myproject", "/content/documents/myproject"));
    }

    @Test
    void match_doubleWildcard_matchesAnyDepth() {
        assertTrue(matcher.match("/content/**", "/content/documents/a/b/c"));
        assertTrue(matcher.match("/content/**", "/content/gallery/images/logo.png"));
    }

    @Test
    void match_doubleWildcard_alsoMatchesParentPrefix() {
        // AntPathMatcher matches /content/** against /content (Spring-style permissive behaviour)
        assertTrue(matcher.match("/content/**", "/content"));
    }

    @Test
    void match_singleWildcard_matchesSingleSegment() {
        assertTrue(matcher.match("/content/*/docs", "/content/documents/docs"));
        assertFalse(matcher.match("/content/*/docs", "/content/documents/sub/docs"));
    }

    @Test
    void match_questionMark_matchesSingleCharacter() {
        assertTrue(matcher.match("/a/b?/c", "/a/bc/c"));
        assertFalse(matcher.match("/a/b?/c", "/a/bcd/c"));
    }

    @Test
    void match_caseInsensitive_matchesIgnoringCase() {
        assertTrue(matcher.match("/Content/**", "/content/documents", false));
    }

    @Test
    void match_caseSensitive_doesNotMatchDifferentCase() {
        assertFalse(matcher.match("/Content/**", "/content/documents", true));
    }

    @Test
    void matchStart_partialPath_returnsTrue() {
        assertTrue(matcher.matchStart("/content/**", "/content"));
        assertTrue(matcher.matchStart("/content/**", "/content/documents"));
    }

    @Test
    void setPathSeparator_customSeparator_usedInMatch() {
        matcher.setPathSeparator(".");
        assertTrue(matcher.match("a.b.**", "a.b.c.d"));
    }

    @Test
    void setPathSeparator_null_resetsToDefault() {
        matcher.setPathSeparator(null);
        assertTrue(matcher.match("/content/**", "/content/documents/test"));
    }
}
