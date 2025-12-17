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
import java.util.concurrent.atomic.AtomicLong;

import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;

class ProcessMonitor {

    private static final int MAX_COMPLETED_PROCESSES = 100;

    private AtomicLong processCounter = new AtomicLong(0L);

    private List<ProcessStatus> processes = Collections.synchronizedList(new ArrayList<>());

    private Map<Long, ProcessStatus> completedProcesses = Collections.synchronizedMap(
            new LinkedHashMap<Long, ProcessStatus>(MAX_COMPLETED_PROCESSES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > MAX_COMPLETED_PROCESSES;
                }
            });

    synchronized ProcessStatus startProcess() {
        ProcessStatus process = new ProcessStatus(processCounter.incrementAndGet(), System.currentTimeMillis());
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

    synchronized ProcessStatus getProcess(long id) {
        for (ProcessStatus process : processes) {
            if (id == process.getId()) {
                return process;
            }
        }
        return completedProcesses.get(id);
    }

    synchronized void clear() {
        processes.clear();
        completedProcesses.clear();
        processCounter.set(0L);
    }
}
