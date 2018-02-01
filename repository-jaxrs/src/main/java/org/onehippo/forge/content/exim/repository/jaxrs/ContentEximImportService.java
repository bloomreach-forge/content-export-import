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
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.hippoecm.repository.util.JcrUtils;
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

/**
 * Content-EXIM Import JAX-RS Service.
 */
@Path("/import")
public class ContentEximImportService extends AbstractContentEximService {

    private static Logger log = LoggerFactory.getLogger(ContentEximImportService.class);

    public ContentEximImportService() {
        super();
    }

    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response importContentFromZip(
            @Multipart(value="batchSize", required=false) String batchSizeParam,
            @Multipart(value="threshold", required=false) String thresholdParam,
            @Multipart(value="publishOnImport", required=false) String publishOnImportParam,
            @Multipart(value="dataUrlSizeThreshold", required=false) String dataUrlSizeThresholdParam,
            @Multipart(value="paramsJson", required=false) String paramsJsonParam,
            @Multipart(value="params", required=false) Attachment paramsAttachment,
            @Multipart(value="package", required=true) Attachment packageAttachment) throws JsonProcessingException {

        Result result = new Result();

        File tempZipFile = null;
        Session session = null;
        ExecutionParams params = new ExecutionParams();

        try {
            tempZipFile = File.createTempFile(TEMP_PREFIX, ".zip");
            log.info("ContentEximService#importContentFromZip begins with {}", tempZipFile.getPath());

            if (packageAttachment == null) {
                result.addError("No zip attachment.");
                return Response.serverError().entity(toJsonString(result)).build();
            }

            if (paramsAttachment != null) {
                final String json = attachmentToString(paramsAttachment, "UTF-8");
                if (StringUtils.isNotBlank(json)) {
                    params = getObjectMapper().readValue(json, ExecutionParams.class);
                }
            } else {
                if (StringUtils.isNotBlank(paramsJsonParam)) {
                    params = getObjectMapper().readValue(paramsJsonParam, ExecutionParams.class);
                }
            }
            overrideExecutionParamsByParameters(params, batchSizeParam, thresholdParam, publishOnImportParam,
                    dataUrlSizeThresholdParam);

            transferAttachmentToFile(packageAttachment, tempZipFile);

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

            batchCount = cleaningMirrorDocbaseValues(session, params, result, batchCount);

            return Response.ok().entity(toJsonString(result)).build();
        } catch (Exception e) {
            log.error("Failed to import content.", e);
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
        final String baseFolderUrlPrefix = baseFolder.getURL().toString();
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
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH,
                        baseFolderUrlPrefix);

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
        final String baseFolderUrlPrefix = baseFolder.getURL().toString();
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
                ContentNodeUtils.prependUrlPrefixInJcrDataValues(contentNode, BINARY_ATTACHMENT_REL_PATH,
                        baseFolderUrlPrefix);

                record = importTask.beginRecord("", path);
                record.setAttribute("file", file.getName().getPath());
                record.setProcessed(true);

                String locale = (contentNode.hasProperty("hippotranslation:locale"))
                        ? contentNode.getProperty("hippotranslation:locale").getValue()
                        : null;
                String localizedName = contentNode.getProperty("jcr:localizedName").getValue();

                String updatedPath = importTask.createOrUpdateDocumentFromVariantContentNode(contentNode,
                        primaryTypeName, path, locale, localizedName);

                if (params.isPublishOnImport()) {
                    importTask.getDocumentManager().publishDocument(updatedPath);
                }

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

    private int cleaningMirrorDocbaseValues(Session session, ExecutionParams params, Result result, int batchCount)
            throws Exception {
        Set<String> mirrorNodePaths = getQueriedNodePaths(session,
                "//element(*)[jcr:like(@hippo:docbase,'/content/%')]", Query.XPATH);
        session.refresh(false);

        for (String mirrorNodePath : mirrorNodePaths) {
            try {
                if (!session.nodeExists(mirrorNodePath)) {
                    continue;
                }

                Node mirrorNode = session.getNode(mirrorNodePath);
                String docbasePath = mirrorNode.getProperty("hippo:docbase").getString();

                if (StringUtils.startsWith(docbasePath, "/") && session.nodeExists(docbasePath)) {
                    JcrUtils.ensureIsCheckedOut(mirrorNode);
                    String docbase = session.getNode(docbasePath).getIdentifier();
                    mirrorNode.setProperty("hippo:docbase", docbase);
                }
            } catch (Exception e) {
                String message = "Failed to clean mirror docbase value at " + mirrorNodePath + ". " + e;
                result.addError(message);
                log.error("Failed to clean mirror docbase value at {}.", mirrorNodePath, e);
            } finally {
                ++batchCount;
                if (batchCount % params.getBatchSize() == 0) {
                    session.save();
                    session.refresh(false);
                    if (params.getThreshold() > 0) {
                        Thread.sleep(params.getThreshold());
                    }
                }
            }
        }

        session.save();
        session.refresh(false);

        return batchCount;
    }
}
