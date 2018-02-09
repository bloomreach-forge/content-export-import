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
package org.onehippo.forge.content.exim.repository.jaxrs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;

/**
 * Content-EXIM Export JAX-RS Service.
 */
@Path("/ps")
public class ContentEximProcessStatusService extends AbstractContentEximService {

    public ContentEximProcessStatusService() {
        super();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getProcessStatusResult() {
        StringWriter sw = new StringWriter(1024);
        PrintWriter out = new PrintWriter(sw);
        out.printf("%8s %5s %15s %8s %8s %5s %s\r\n", "UID", "PID", "TTY", "STIME", "TIME", "%PRGR", "CMD");

        if (getProcessMonitor() != null) {
            List<ProcessStatus> processes = getProcessMonitor().getProcesses();
            FastDateFormat timeFormat = FastDateFormat.getInstance("HH:mm:ss");

            for (ProcessStatus process : processes) {
                final long startTime = process.getStartTimeMillis();
                final long duration = System.currentTimeMillis() - startTime;
                out.printf("%8s %5d %15s %8s %8s %1.2f  %s\r\n", process.getUsername(), process.getId(),
                        process.getClientInfo(), timeFormat.format(startTime),
                        DurationFormatUtils.formatDuration(duration, "HH:mm:ss"), process.getProgress(),
                        process.getCommandInfo());
            }
        }

        out.print("\r\n");

        return sw.toString();
    }
}
