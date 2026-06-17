/*
 * Copyright 2025 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.repository.jaxrs.status;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessStatusTest {

    @Test
    void constructor_setsIdAndStartTime() {
        long now = System.currentTimeMillis();
        ProcessStatus status = new ProcessStatus(42L, now);
        assertEquals(42L, status.getId());
        assertEquals(now, status.getStartTimeMillis());
    }

    @Test
    void defaultStringFields_areNull() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        assertNull(status.getUsername());
        assertNull(status.getClientInfo());
        assertNull(status.getCommandInfo());
        assertNull(status.getLogFile());
        assertNull(status.getExecutionParams());
    }

    @Test
    void setUsername_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        status.setUsername("admin");
        assertEquals("admin", status.getUsername());
    }

    @Test
    void setClientInfo_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        status.setClientInfo("127.0.0.1");
        assertEquals("127.0.0.1", status.getClientInfo());
    }

    @Test
    void setCommandInfo_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        status.setCommandInfo("export");
        assertEquals("export", status.getCommandInfo());
    }

    @Test
    void setProgress_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        status.setProgress(0.75);
        assertEquals(0.75, status.getProgress(), 0.0001);
    }

    @Test
    void setExecutionParams_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        ExecutionParams params = new ExecutionParams();
        status.setExecutionParams(params);
        assertEquals(params, status.getExecutionParams());
    }

    @Test
    void setLogFile_andGet_roundtrips() {
        ProcessStatus status = new ProcessStatus(1L, 0L);
        File logFile = new File("/tmp/exim.log");
        status.setLogFile(logFile);
        assertEquals(logFile, status.getLogFile());
    }
}
