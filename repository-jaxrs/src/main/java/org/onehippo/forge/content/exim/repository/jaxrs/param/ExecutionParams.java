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

public class ExecutionParams {

    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final long DEFAULT_THRESHOLD = 10L;
    private static final long DEFAULT_DATA_URL_SIZE_THRESHOLD = 256 * 1024;

    private Integer batchSize;
    private Long threshold;
    private Long dataUrlSizeThreshold;
    private QueriesAndPaths binaries;
    private QueriesAndPaths documents;

    public Integer getBatchSize() {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Long getThreshold() {
        if (threshold == null) {
            return DEFAULT_THRESHOLD;
        }
        return threshold;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    public Long getDataUrlSizeThreshold() {
        if (dataUrlSizeThreshold == null) {
            return DEFAULT_DATA_URL_SIZE_THRESHOLD;
        }
        return dataUrlSizeThreshold;
    }

    public void setDataUrlSizeThreshold(Long dataUrlSizeThreshold) {
        this.dataUrlSizeThreshold = dataUrlSizeThreshold;
    }

    public QueriesAndPaths getBinaries() {
        return binaries;
    }

    public void setBinaries(QueriesAndPaths binaries) {
        this.binaries = binaries;
    }

    public QueriesAndPaths getDocuments() {
        return documents;
    }

    public void setDocuments(QueriesAndPaths documents) {
        this.documents = documents;
    }

}
