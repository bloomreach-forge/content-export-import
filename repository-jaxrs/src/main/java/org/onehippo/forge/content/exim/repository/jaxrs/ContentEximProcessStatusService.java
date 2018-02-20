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

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.tika.io.IOUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.status.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content-EXIM PS JAX-RS Service.
 */
@Path("/ps")
public class ContentEximProcessStatusService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximProcessStatusService.class);

    private FastDateFormat timeFormat = FastDateFormat.getInstance("HH:mm:ss");

    public ContentEximProcessStatusService() {
        super();
    }

    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getAllProcessInfos() {
        StringWriter sw = new StringWriter(1024);
        PrintWriter out = new PrintWriter(sw);
        printProcessStatusReportHeader(out);

        if (getProcessMonitor() != null) {
            List<ProcessStatus> processes = getProcessMonitor().getProcesses();

            for (ProcessStatus process : processes) {
                final long startTime = process.getStartTimeMillis();
                final long duration = System.currentTimeMillis() - startTime;
                out.printf("%8s %5d %15s %8s %8s %1.2f  %s\r\n", process.getUsername(), process.getId(),
                        process.getClientInfo(), timeFormat.format(startTime),
                        DurationFormatUtils.formatDuration(duration, "HH:mm:ss"), process.getProgress(),
                        process.getCommandInfo());
            }
        }

        printProcessStatusReportFooter(out);

        return sw.toString();
    }

    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getProcessInfo(@PathParam("id") long processId) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter out = new PrintWriter(sw);

        printProcessStatusReportHeader(out);

        if (getProcessMonitor() != null) {
            ProcessStatus process = getProcessMonitor().getProcess(processId);

            if (process != null) {
                printProcessStatus(out, process);
                final ExecutionParams params = process.getExecutionParams();

                if (params != null) {
                    out.print("\r\n");
                    out.print("\r\n");
                    out.printf("%11s\r\n", "PARAMS");
                    out.print("\r\n");
                    printExecutionParams(out, params);
                    out.print("\r\n");
                    out.print("\r\n");
                }
            }
        }

        printProcessStatusReportFooter(out);

        return sw.toString();
    }

    @Path("/{id}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getLogsOfProcess(@PathParam("id") long processId) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter out = new PrintWriter(sw);

        if (getProcessMonitor() != null) {
            ProcessStatus process = getProcessMonitor().getProcess(processId);

            if (process != null) {
                File logFile = process.getLogFile();

                if (logFile != null && logFile.isFile()) {
                    printLogFile(out, logFile);
                    out.print("\r\n");
                }
            }
        }

        return sw.toString();
    }

    private void printProcessStatusReportHeader(PrintWriter out) {
        out.printf("%8s %5s %15s %8s %8s %5s %s\r\n", "UID", "PID", "TTY", "STIME", "TIME", "%PRGR", "CMD");
    }

    private void printProcessStatusReportFooter(PrintWriter out) {
        out.print("\r\n");
    }

    private void printProcessStatus(PrintWriter out, ProcessStatus process) {
        final long startTime = process.getStartTimeMillis();
        final long duration = System.currentTimeMillis() - startTime;

        out.printf("%8s %5d %15s %8s %8s %1.2f  %s\r\n", process.getUsername(), process.getId(),
                process.getClientInfo(), timeFormat.format(startTime),
                DurationFormatUtils.formatDuration(duration, "HH:mm:ss"), process.getProgress(),
                process.getCommandInfo());
    }

    private void printExecutionParams(PrintWriter out, ExecutionParams params) {
        try {
            getObjectMapper().writeValue(out, params);
        } catch (Exception e) {
            log.error("Failed to write execution params.", e);
        }
    }

    private void printLogFile(PrintWriter out, File logFile) {
        FileReader fr = null;

        try {
            fr = new FileReader(logFile);
            IOUtils.copy(fr, out);
        } catch (Exception e) {
            log.error("Failed to read log file.", e);
        } finally {
            IOUtils.closeQuietly(fr);
        }
    }
}
