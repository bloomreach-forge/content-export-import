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

    public int getTotalBinaryCount() {
        return totalBinaryCount;
    }

    public void setTotalBinaryCount(int totalBinaryCount) {
        this.totalBinaryCount = totalBinaryCount;
    }

    public int getTotalDocumentCount() {
        return totalDocumentCount;
    }

    public void setTotalDocumentCount(int totalDocumentCount) {
        this.totalDocumentCount = totalDocumentCount;
    }

    public int getSucceededBinaryCount() {
        return succeededBinaryCount;
    }

    public void setSucceededBinaryCount(int succeededBinaryCount) {
        this.succeededBinaryCount = succeededBinaryCount;
    }

    public int getSucceededDocumentCount() {
        return succeededDocumentCount;
    }

    public void setSucceededDocumentCount(int succeededDocumentCount) {
        this.succeededDocumentCount = succeededDocumentCount;
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
}
