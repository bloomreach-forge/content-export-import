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

import javax.jcr.Value;

import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.pojo.common.ContentValueConverter;
import org.onehippo.forge.content.pojo.common.jcr.DefaultJcrContentValueConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract public class AbstractContentExportImportTask {

    private final DocumentManager documentManager;
    private ObjectMapper objectMapper;
    private ContentValueConverter<Value> contentValueConverter;

    public AbstractContentExportImportTask(final DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    public DocumentManager getDocumentManager() {
        return documentManager;
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

    public ContentValueConverter<Value> getContentValueConverter() {
        if (contentValueConverter == null) {
            contentValueConverter = new DefaultJcrContentValueConverter(getDocumentManager().getSession());
        }

        return contentValueConverter;
    }

    public void setContentValueConverter(ContentValueConverter<Value> contentValueConverter) {
        this.contentValueConverter = contentValueConverter;
    }

}
