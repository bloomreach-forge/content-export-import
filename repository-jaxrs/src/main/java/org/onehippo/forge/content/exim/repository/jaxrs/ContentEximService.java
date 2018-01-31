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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.impl.DefaultBinaryExportTask;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentManagerImpl;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentVariantExportTask;
import org.onehippo.forge.content.exim.core.util.ContentNodeUtils;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExportParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ContentItemSetCollector;
import org.onehippo.forge.content.exim.repository.jaxrs.util.ZipCompressUtils;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class ContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximService.class);

    private static final String ZIP_TEMP_BASE_FOLDER_PREFIX = "_content_exim_";

    private ObjectMapper objectMapper = new ObjectMapper();

    private Session session;

    public ContentEximService() {
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Path("/export")
    @Consumes("application/json")
    @Produces("application/octet-stream")
    @POST
    public StreamingOutput exportContentToZip(String exportParamsJson, @Context HttpServletResponse response) {
        File baseFolder = null;

        try {
            ExportParams params = objectMapper.readValue(exportParamsJson, ExportParams.class);
            Result result = ContentItemSetCollector.collectItemsFromExportParams(getSession(), params);
            DocumentManager documentManager = new WorkflowDocumentManagerImpl(getSession());
            baseFolder = Files.createTempDirectory(ZIP_TEMP_BASE_FOLDER_PREFIX).toFile();
            FileObject baseFolderObject = VFS.getManager().resolveFile(baseFolder.toURI());

            exportBinaries(documentManager, result, baseFolderObject);
            exportDocuments(documentManager, result, baseFolderObject);

            String fileName = "content-exim-export-" + DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd-HHmmss") + ".zip";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            final File zipBaseFolder = baseFolder;

            return new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    ZipArchiveOutputStream zaos = null;
                    try {
                        zaos = new ZipArchiveOutputStream(output);
                        ZipCompressUtils.compressFolderToZip(zipBaseFolder, "", zaos);
                    } finally {
                        zaos.finish();
                        IOUtils.closeQuietly(zaos);
                        FileUtils.deleteDirectory(zipBaseFolder);
                    }
                }
            };
        } catch (Exception e) {
            if (baseFolder != null) {
                try {
                    FileUtils.deleteDirectory(baseFolder);
                } catch (Exception ioe) {
                    log.error("Failed to delete the temporary folder at {}", baseFolder.getPath(), e);
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final String message = new StringBuilder().append(e.getMessage()).append("\r\n").toString();
            return new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    output.write(message.getBytes());
                }
            };
        }
    }

    @Path("/import")
    @Consumes("application/octet-stream")
    @Produces("application/json")
    @POST
    public Response importContentFromZip() {
        try {
            Result result = new Result();
            result.addItem(new ResultItem("/a/b/c", "nt1:type1"));
            return Response.ok(objectMapper.writeValueAsString(result)).build();
        } catch (Exception e) {
            final String message = new StringBuilder().append(e.getMessage()).toString();
            return Response.serverError().entity(message).build();
        }
    }

    private void exportBinaries(DocumentManager documentManager, Result result, FileObject baseFolder)
            throws Exception {
        DefaultBinaryExportTask exportTask = new DefaultBinaryExportTask(documentManager);

        exportTask.setLogger(log);
        exportTask.setBinaryValueFileFolder(baseFolder);
        exportTask.setDataUrlSizeThreashold(512 * 1024); // 512KB as threshold
        exportTask.start();

        for (ResultItem item : result.getItems()) {
            ContentMigrationRecord record = null;

            try {
                String handlePath = item.getPath();

                if (!HippoNodeUtils.isBinaryPath(handlePath)) {
                    continue;
                }

                if (!documentManager.getSession().nodeExists(handlePath)) {
                    continue;
                }

                Node handle = documentManager.getSession().getNode(handlePath);
                Node variant = HippoNodeUtils.getFirstVariantNode(handle);

                if (variant == null) {
                    continue;
                }

                String variantPath = variant.getPath();
                record = exportTask.beginRecord(variant.getIdentifier(), variantPath);

                String relPath = StringUtils.removeStart(ContentPathUtils.removeIndexNotationInNodePath(variantPath),
                        "/");
                FileObject file = baseFolder.resolveFile(relPath + ".json");

                record.setProcessed(true);
                ContentNode contentNode = exportTask.exportBinarySetToContentNode(variant);
                ContentNodeUtils.replaceDocbasesByPaths(documentManager.getSession(), contentNode,
                        ContentNodeUtils.MIRROR_DOCBASES_XPATH);
                record.setAttribute("file", file.getName().getPath());

                exportTask.writeContentNodeToJsonFile(contentNode, file);
                log.debug("Exported document from {} to {}.", handlePath, file.getName().getPath());
                record.setSucceeded(true);
            } catch (Exception e) {
                log.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    exportTask.endRecord();
                }
            }
        }

        exportTask.stop();
        exportTask.logSummary();
    }

    private void exportDocuments(DocumentManager documentManager, Result result, FileObject baseFolder)
            throws Exception {
        WorkflowDocumentVariantExportTask exportTask = new WorkflowDocumentVariantExportTask(documentManager);

        exportTask.setLogger(log);
        exportTask.setBinaryValueFileFolder(baseFolder);
        exportTask.setDataUrlSizeThreashold(512 * 1024); // 512KB as threshold
        exportTask.start();

        for (ResultItem item : result.getItems()) {
            ContentMigrationRecord record = null;

            try {
                String handlePath = item.getPath();

                if (!HippoNodeUtils.isBinaryPath(handlePath)) {
                    continue;
                }

                if (!documentManager.getSession().nodeExists(handlePath)) {
                    continue;
                }

                Node handle = documentManager.getSession().getNode(handlePath);
                Map<String, Node> variantsMap = HippoNodeUtils.getDocumentVariantsMap(handle);
                Node variant = variantsMap.get(HippoStdNodeType.PUBLISHED);
                if (variant == null) {
                    variant = variantsMap.get(HippoStdNodeType.UNPUBLISHED);
                }

                if (variant == null) {
                    continue;
                }

                String variantPath = variant.getPath();
                record = exportTask.beginRecord(variant.getIdentifier(), variantPath);

                String relPath = StringUtils.removeStart(ContentPathUtils.removeIndexNotationInNodePath(variantPath),
                        "/");
                FileObject file = baseFolder.resolveFile(relPath + ".json");

                Document document = new Document(variant.getIdentifier());

                record.setProcessed(true);
                ContentNode contentNode = exportTask.exportVariantToContentNode(document);
                ContentNodeUtils.replaceDocbasesByPaths(documentManager.getSession(), contentNode,
                        ContentNodeUtils.MIRROR_DOCBASES_XPATH);

                record.setAttribute("file", file.getName().getPath());

                exportTask.writeContentNodeToJsonFile(contentNode, file);
                log.debug("Exported document from {} to {}.", handlePath, file.getName().getPath());
                record.setSucceeded(true);
            } catch (Exception e) {
                log.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    exportTask.endRecord();
                }
            }
        }

        exportTask.stop();
        exportTask.logSummary();
    }

}
