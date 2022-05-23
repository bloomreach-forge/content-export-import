/*
 * Copyright 2016-2022 Bloomreach B.V. (https://www.bloomreach.com)
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
 * DocumentManagerNotFoundException thrown by {@link DocumentManager},
 * when a folder or document is not found by a specified location information.
 */
public class DocumentManagerNotFoundException extends DocumentManagerException {

    private static final long serialVersionUID = 1L;

    public DocumentManagerNotFoundException() {
        super();
    }

    public DocumentManagerNotFoundException(String message) {
        super(message);
    }

    public DocumentManagerNotFoundException(Throwable nested) {
        super(nested);
    }

    public DocumentManagerNotFoundException(String msg, Throwable nested) {
        super(msg, nested);
    }

}
