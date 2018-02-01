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

import javax.jcr.Session;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.onehippo.forge.content.exim.core.ContentMigrationRecord;
import org.onehippo.forge.content.exim.core.DocumentManager;
import org.onehippo.forge.content.exim.core.impl.DefaultBinaryImportTask;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentManagerImpl;
import org.onehippo.forge.content.exim.core.impl.WorkflowDocumentVariantImportTask;
import org.onehippo.forge.content.exim.core.util.ContentNodeUtils;
import org.onehippo.forge.content.exim.core.util.ContentPathUtils;
import org.onehippo.forge.content.exim.core.util.HippoBinaryNodeUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/import")
public class ContentEximImportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximImportService.class);

    public ContentEximImportService() {
        super();
    }

    @Path("/")
    @Produces("application/json")
    @POST
    public Response importContentFromZip(MultipartBody body) throws JsonProcessingException {
        Result result = new Result();

        File tempZipFile = null;
        Session session = null;

        try {
            ExecutionParams params = new ExecutionParams();
            Attachment paramsAttachment = body.getAttachment("params");

            if (paramsAttachment != null) {
                final String json = attachmentToString(paramsAttachment, "UTF-8");
                params = getObjectMapper().readValue(json, ExecutionParams.class);
            }

            Attachment zipAttachment = body.getAttachment("zip");

            if (zipAttachment == null) {
                result.addError("No zip attachment.");
                return Response.serverError().entity(toJsonString(result)).build();
            }

            tempZipFile = File.createTempFile(ZIP_TEMP_BASE_PREFIX, ".zip");
            log.info("ContentEximService#importContentFromZip begins with {}", tempZipFile.getPath());

            zipAttachment.transferTo(tempZipFile);
            FileObject baseFolder = VFS.getManager().resolveFile("zip:" + tempZipFile.toURI());

            session = createSession();

            DocumentManager documentManager = new WorkflowDocumentManagerImpl(session);

            final DefaultBinaryImportTask binaryImportTask = new DefaultBinaryImportTask(documentManager);
            binaryImportTask.setLogger(log);

            final WorkflowDocumentVariantImportTask documentImportTask = new WorkflowDocumentVariantImportTask(
                    documentManager);
            documentImportTask.setLogger(log);

            int batchCount = 0;

            try {
                binaryImportTask.start();
                batchCount = importBinaries(params, baseFolder, binaryImportTask, result, batchCount);
            } finally {
                binaryImportTask.stop();
            }

            try {
                documentImportTask.start();
                batchCount = importDocuments(params, baseFolder, documentImportTask, result, batchCount);
            } finally {
                documentImportTask.stop();
            }

            return Response.ok().entity(toJsonString(result)).build();
        } catch (Exception e) {
            result.addError(e.toString());
            return Response.serverError().entity(toJsonString(result)).build();
        } finally {
            log.info("ContentEximService#importContentFromZip ends.");
            if (tempZipFile != null) {
                tempZipFile.delete();
            }
            if (session != null) {
                session.logout();
            }
        }
    }

    private int importBinaries(ExecutionParams params, FileObject baseFolder, DefaultBinaryImportTask importTask,
            Result result, int batchCount) throws Exception {
        final String baseFolderUrlPrefix = StringUtils.removeEnd(baseFolder.getURL().toString(), "/") + "/";
        FileObject[] files = importTask.findFilesByNamePattern(baseFolder, "^.+\\.json$", 1, 20);

        for (FileObject file : files) {
            if (isStopRequested(baseFolder)) {
                log.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(), STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentNode contentNode = importTask.readContentNodeFromJsonFile(file);

            String primaryTypeName = contentNode.getPrimaryType();
            String path = contentNode.getProperty("jcr:path").getValue();

            if (!HippoNodeUtils.isBinaryPath(path)) {
                continue;
            }

            ContentMigrationRecord record = null;

            try {
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH, baseFolderUrlPrefix);

                record = importTask.beginRecord("", path);
                record.setAttribute("file", file.getName().getPath());
                record.setProcessed(true);

                String[] folderPathAndName = ContentPathUtils.splitToFolderPathAndName(path);
                String folderPath = folderPathAndName[0];
                String name = folderPathAndName[1];

                String folderPrimaryType = DEFAULT_GALLERY_FOLDER_PRIMARY_TYPE;
                String[] folderTypes = DEFAULT_GALLERY_FOLDER_FOLDER_TYPES;
                String[] galleryTypes = DEFAULT_GALLERY_GALLERY_TYPES;

                if (HippoNodeUtils.isAssetPath(path)) {
                    folderPrimaryType = DEFAULT_ASSET_FOLDER_PRIMARY_TYPE;
                    folderTypes = DEFAULT_ASSET_FOLDER_FOLDER_TYPES;
                    galleryTypes = DEFAULT_ASSET_GALLERY_TYPES;
                }

                folderPath = importTask.createOrUpdateBinaryFolder(folderPath, folderPrimaryType, folderTypes,
                        galleryTypes);

                String updatedPath = importTask.createOrUpdateBinaryFromContentNode(contentNode, primaryTypeName,
                        folderPath, name);

                HippoBinaryNodeUtils.extractTextFromBinariesAndSaveHippoTextsUnderHandlePath(
                        importTask.getDocumentManager().getSession(), updatedPath);

                record.setSucceeded(true);
            } catch (Exception e) {
                log.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    importTask.endRecord();
                    result.addItem(recordToResultItem(record));
                    result.incrementTotalBinaryCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededBinaryCount();
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    importTask.getDocumentManager().getSession().save();
                    importTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThreshold() > 0) {
                        Thread.sleep(params.getThreshold());
                    }
                }
            }
        }

        importTask.getDocumentManager().getSession().save();
        importTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }

    private int importDocuments(ExecutionParams params, FileObject baseFolder,
            WorkflowDocumentVariantImportTask importTask, Result result, int batchCount) throws Exception {
        final String baseFolderUrlPrefix = StringUtils.removeEnd(baseFolder.getURL().toString(), "/") + "/";
        FileObject[] files = importTask.findFilesByNamePattern(baseFolder, "^.+\\.json$", 1, 20);

        for (FileObject file : files) {
            if (isStopRequested(baseFolder)) {
                log.info("Stop requested by file at {}/{}", baseFolder.getName().getPath(), STOP_REQUEST_FILE_REL_PATH);
                break;
            }

            ContentNode contentNode = importTask.readContentNodeFromJsonFile(file);

            String primaryTypeName = contentNode.getPrimaryType();
            String path = contentNode.getProperty("jcr:path").getValue();

            if (!HippoNodeUtils.isDocumentPath(path)) {
                continue;
            }

            ContentMigrationRecord record = null;

            try {
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH, baseFolderUrlPrefix);

                record = importTask.beginRecord("", path);
                record.setAttribute("file", file.getName().getPath());
                record.setProcessed(true);

                String locale = (contentNode.hasProperty("hippotranslation:locale")) ? contentNode.getProperty("hippotranslation:locale").getValue() : null;
                String localizedName = contentNode.getProperty("jcr:localizedName").getValue();

                String updatedPath = importTask.createOrUpdateDocumentFromVariantContentNode(contentNode,
                        primaryTypeName, path, locale, localizedName);

                record.setSucceeded(true);
            } catch (Exception e) {
                log.error("Failed to process record: {}", record, e);
                if (record != null) {
                    record.setErrorMessage(e.toString());
                }
            } finally {
                if (record != null) {
                    importTask.endRecord();
                    result.addItem(recordToResultItem(record));
                    result.incrementTotalDocumentCount();
                    if (record.isSucceeded()) {
                        result.incrementSucceededDocumentCount();
                    }
                }
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    importTask.getDocumentManager().getSession().save();
                    importTask.getDocumentManager().getSession().refresh(false);
                    if (params.getThreshold() > 0) {
                        Thread.sleep(params.getThreshold());
                    }
                }

            }
        }

        importTask.getDocumentManager().getSession().save();
        importTask.getDocumentManager().getSession().refresh(false);

        return batchCount;
    }

}
