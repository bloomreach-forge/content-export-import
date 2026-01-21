/*
 * Copyright 2024 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.repository.jaxrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;

/**
 * Manages the lifecycle of asynchronous import/export processes.
 *
 * <p>Process caching behavior:
 * <ul>
 *   <li>Running processes are stored in a synchronized list</li>
 *   <li>Completed processes (failed, cancelled, or succeeded) are cached in an LRU LinkedHashMap with max 100 items</li>
 *   <li>When completed processes exceed 100 items, the least recently accessed process is evicted</li>
 *   <li>Process files have a separate 24-hour TTL managed by ProcessFileManager</li>
 *   <li>Note: Process status may be evicted from cache before its file TTL expires if more than 100 processes complete</li>
 * </ul>
 *
 * <p>Process states:
 * <ul>
 *   <li>RUNNING: Process is actively executing</li>
 *   <li>COMPLETED: Process finished successfully</li>
 *   <li>FAILED: Process encountered an error and stopped</li>
 *   <li>CANCELLED: Process was cancelled by user request</li>
 * </ul>
 */
class ProcessMonitor {

    private static final int MAX_COMPLETED_PROCESSES = 100;

    private List<ProcessStatus> processes = Collections.synchronizedList(new ArrayList<>());

    private Map<String, ProcessStatus> completedProcesses = Collections.synchronizedMap(
            new LinkedHashMap<String, ProcessStatus>(MAX_COMPLETED_PROCESSES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > MAX_COMPLETED_PROCESSES;
                }
            });

    synchronized ProcessStatus startProcess() {
        ProcessStatus process = new ProcessStatus(UUID.randomUUID().toString(), System.currentTimeMillis());
        processes.add(process);
        return process;
    }

    synchronized void stopProcess(ProcessStatus process) {
        if (process != null) {
            processes.remove(process);
            if (process.getStatus() != ProcessStatus.Status.RUNNING) {
                completedProcesses.put(process.getId(), process);
            }
        }
    }

    synchronized List<ProcessStatus> getProcesses() {
        List<ProcessStatus> list = new ArrayList<>();
        list.addAll(processes);
        return Collections.unmodifiableList(list);
    }

    synchronized ProcessStatus getProcess(String id) {
        for (ProcessStatus process : processes) {
            if (id.equals(process.getId())) {
                return process;
            }
        }
        return completedProcesses.get(id);
    }

    synchronized void clear() {
        processes.clear();
        completedProcesses.clear();
    }

    /**
     * Checks for and removes stale running processes that have exceeded the timeout duration.
     * This handles cases where processes may have crashed or become orphaned.
     *
     * @param timeoutMillis timeout duration in milliseconds; processes running longer than this are marked as failed
     * @return list of processes that were marked as failed due to timeout
     */
    synchronized List<ProcessStatus> cleanupStaledProcesses(long timeoutMillis) {
        List<ProcessStatus> staledProcesses = new ArrayList<>();
        long currentTimeMillis = System.currentTimeMillis();

        for (ProcessStatus process : new ArrayList<>(processes)) {
            if (process.getStatus() == ProcessStatus.Status.RUNNING) {
                long duration = currentTimeMillis - process.getStartTimeMillis();
                if (duration > timeoutMillis) {
                    process.setStatus(ProcessStatus.Status.FAILED);
                    process.setErrorMessage("Process exceeded maximum execution time and was terminated");
                    process.setCompletionTimeMillis(currentTimeMillis);
                    stopProcess(process);
                    staledProcesses.add(process);
                }
            }
        }

        return staledProcesses;
    }
}
