/*
 * Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.vfs2.FileDepthSelector;
import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.util.Messages;

/**
 * A {@link FileSelector} that selects all files in a particular depth range,
 * with filtering a file by the given {@link FileFilter} instance.
 */
public class FileFilterDepthSelector extends FileDepthSelector {

    /**
     * The FileFilter.
     */
    private final FileFilter fileFilter;

    public FileFilterDepthSelector(final FileFilter fileFilter, int minDepth, int maxDepth) {
        super(minDepth, maxDepth);
        this.fileFilter = fileFilter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeFile(final FileSelectInfo fileInfo) {
        if (!super.includeFile(fileInfo)) {
            return false;
        }

        return accept(fileInfo);
    }

    /**
     * Check if {@code fileInfo} is acceptable by using the internal filter.
     * @param fileInfo fileInfo
     * @return true if {@code fileInfo} is acceptable by using the internal filter
     */
    protected boolean accept(final FileSelectInfo fileInfo) {
        if (fileFilter != null) {
            return fileFilter.accept(fileInfo);
        }

        throw new IllegalArgumentException(Messages.getString("vfs.selectors/filefilter.missing.error"));
    }
}
