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
package org.onehippo.forge.content.exim.repository.jaxrs.status;

import java.io.File;

import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;

public class ProcessStatus {

    private final long id;
    private final long startTimeMillis;
    private String username;
    private String clientInfo;
    private String commandInfo;
    private double progress;
    private ExecutionParams executionParams;
    private File logFile;

    public ProcessStatus(final long id, final long startTimeMillis) {
        this.id = id;
        this.startTimeMillis = startTimeMillis;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getCommandInfo() {
        return commandInfo;
    }

    public void setCommandInfo(String commandInfo) {
        this.commandInfo = commandInfo;
    }

    public long getId() {
        return id;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public ExecutionParams getExecutionParams() {
        return executionParams;
    }

    public void setExecutionParams(ExecutionParams executionParams) {
        this.executionParams = executionParams;
    }

    public File getLogFile() {
        return logFile;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

}
