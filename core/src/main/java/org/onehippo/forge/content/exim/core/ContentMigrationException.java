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
package org.onehippo.forge.content.exim.core;

/**
 * ContentMigrationException thrown by {@link ContentMigrationTask}.
 */
public class ContentMigrationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContentMigrationException() {
        super();
    }

    public ContentMigrationException(String message) {
        super(message);
    }

    public ContentMigrationException(Throwable nested) {
        super(nested);
    }

    public ContentMigrationException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
