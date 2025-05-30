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
package org.onehippo.forge.content.exim.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;

/**
 * File name regular expression based {@link FileFilter} implementation.
 */
public class NamePatternFileFilter implements FileFilter {

    private final Pattern namePattern;

    public NamePatternFileFilter(final Pattern namePattern) {
        this.namePattern = namePattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(FileSelectInfo fileInfo) {
        final String name = fileInfo.getFile().getName().getBaseName();
        final Matcher m = namePattern.matcher(name);
        return m.matches();
    }

}
