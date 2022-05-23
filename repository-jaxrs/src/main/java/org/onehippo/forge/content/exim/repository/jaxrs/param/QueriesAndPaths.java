/*
 * Copyright 2022 Bloomreach B.V. (https://www.bloomreach.com)
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
import java.util.List;

/**
 * Queries and Paths.
 */
public class QueriesAndPaths {

    private List<String> queries;
    private List<String> paths;
    private List<String> includes;
    private List<String> excludes;

    public List<String> getQueries() {
        if (queries == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(queries);
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    public List<String> getPaths() {
        if (paths == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(paths);
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getIncludes() {
        if (includes == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(includes);
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        if (excludes == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(excludes);
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

}
