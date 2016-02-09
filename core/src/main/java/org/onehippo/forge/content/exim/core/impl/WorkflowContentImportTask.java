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
package org.onehippo.forge.content.exim.core.impl;

import javax.jcr.Node;
import javax.jcr.Value;

import org.apache.commons.vfs2.FileObject;
import org.onehippo.forge.content.exim.core.ContentExportException;
import org.onehippo.forge.content.exim.core.ContentImportTask;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.binder.ContentNodeBinder;
import org.onehippo.forge.content.pojo.binder.ContentNodeBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultContentNodeJcrBindingItemFilter;
import org.onehippo.forge.content.pojo.binder.jcr.DefaultJcrContentNodeBinder;
import org.onehippo.forge.content.pojo.model.ContentItem;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WorkflowContentImportTask implements ContentImportTask {

    private final DocumentManager documentManager;

    private ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder;
    private ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter;
    private ObjectMapper objectMapper;

    public WorkflowContentImportTask(final DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    public ContentNodeBinder<Node, ContentItem, Value> getContentNodeBinder() {
        if (contentNodeBinder == null) {
            contentNodeBinder = new DefaultJcrContentNodeBinder();
        }

        return contentNodeBinder;
    }

    public void setContentNodeBinder(ContentNodeBinder<Node, ContentItem, Value> contentNodeBinder) {
        this.contentNodeBinder = contentNodeBinder;
    }

    public ContentNodeBindingItemFilter<ContentItem> getContentNodeBindingItemFilter() {
        if (contentNodeBindingItemFilter == null) {
            contentNodeBindingItemFilter = new DefaultContentNodeJcrBindingItemFilter();
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter).addPropertyPathExclude("hippo:*");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostd:*");
            ((DefaultContentNodeJcrBindingItemFilter) contentNodeBindingItemFilter)
                    .addPropertyPathExclude("hippostdpubwf:*");
        }

        return contentNodeBindingItemFilter;
    }

    public void setContentNodeBindingItemFilter(
            ContentNodeBindingItemFilter<ContentItem> contentNodeBindingItemFilter) {
        this.contentNodeBindingItemFilter = contentNodeBindingItemFilter;
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    @Override
    public void importVariantJsonFileToHandle(FileObject targetFile, String handlePath)
            throws ContentExportException {
        //getDocumentManager().createDocument(folderLocation, templateCategory, prototype, nodeName, locale, displayName);
    }

}
