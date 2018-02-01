/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Result {

    private int totalBinaryCount;
    private int totalDocumentCount;
    private int succeededBinaryCount;
    private int succeededDocumentCount;
    private List<ResultItem> items = new LinkedList<>();
    private List<String> errors;

    public int getTotalBinaryCount() {
        return totalBinaryCount;
    }

    public void setTotalBinaryCount(int totalBinaryCount) {
        this.totalBinaryCount = totalBinaryCount;
    }

    public int incrementTotalBinaryCount() {
        return ++totalBinaryCount;
    }

    public int getTotalDocumentCount() {
        return totalDocumentCount;
    }

    public void setTotalDocumentCount(int totalDocumentCount) {
        this.totalDocumentCount = totalDocumentCount;
    }

    public int incrementTotalDocumentCount() {
        return ++totalDocumentCount;
    }

    public int getSucceededBinaryCount() {
        return succeededBinaryCount;
    }

    public void setSucceededBinaryCount(int succeededBinaryCount) {
        this.succeededBinaryCount = succeededBinaryCount;
    }

    public int incrementSucceededBinaryCount() {
        return ++succeededBinaryCount;
    }

    public int getSucceededDocumentCount() {
        return succeededDocumentCount;
    }

    public void setSucceededDocumentCount(int succeededDocumentCount) {
        this.succeededDocumentCount = succeededDocumentCount;
    }

    public int incrementSucceededDocumentCount() {
        return ++succeededDocumentCount;
    }

    public List<ResultItem> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(items);
    }

    public void setItems(List<ResultItem> items) {
        if (items == null) {
            this.items = null;
        } else {
            this.items = new LinkedList<>(items);
        }
    }

    public void addItem(ResultItem item) {
        if (items == null) {
            items = new LinkedList<>();
        }

        items.add(item);
    }

    public List<String> getErrors() {
        if (errors == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(errors);
    }

    public void setErrors(List<String> errors) {
        if (errors == null) {
            this.errors = null;
        } else {
            this.errors = new LinkedList<>(errors);
        }
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new LinkedList<>();
        }

        errors.add(error);
    }

}
