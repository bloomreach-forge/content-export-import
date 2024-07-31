/*
 * Copyright 2015-2024 Bloomreach B.V. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.updater

import java.io.*
import javax.jcr.*
import org.apache.commons.io.*
import org.apache.commons.lang.*
import org.apache.commons.lang.text.*
import org.apache.commons.vfs2.*
import org.apache.jackrabbit.util.*
import org.hippoecm.repository.api.*
import org.onehippo.repository.update.BaseNodeUpdateVisitor
import org.onehippo.forge.content.pojo.model.*
import org.onehippo.forge.content.exim.core.*
import org.onehippo.forge.content.exim.core.impl.*
import org.onehippo.forge.content.exim.core.util.*
import org.onehippo.forge.gallerymagick.core.*
import org.onehippo.forge.gallerymagick.core.command.*

class ImportingImageFilesUpdateVisitor extends BaseNodeUpdateVisitor {

  def documentManager
  def importTask
  def sourceBaseFolder
  def targetBaseFolderNodePath
  def imageProcessor
  def extensionMimeTypes

  void initialize(Session session) {
    def sourceBaseFolderPath = StrSubstitutor.replaceSystemProperties(parametersMap.get("sourceBaseFolderPath"))
    sourceBaseFolder = VFS.getManager().resolveFile(sourceBaseFolderPath)

    targetBaseFolderNodePath = parametersMap.get("targetBaseFolderNodePath")

    imageProcessor = parametersMap.get("imageProcessor")

    extensionMimeTypes = parametersMap.get("extensionMimeTypes")

    documentManager = new WorkflowDocumentManagerImpl(session)
    importTask = new DefaultBinaryImportTask(documentManager)
    importTask.setLogger(log)
    importTask.start()
  }

  boolean doUpdate(Node node) {
    def contentNode
    def binaryPrimaryTypeName = "hippogallery:imageset"
    def mimeType
    def originalNode
    def thumbnailNode

    def binaryFolderPrimaryTypeName
    def binaryFolderFolderTypes
    def binaryFolderGalleryTypes
    def binaryLocation
    def binaryFolderPath
    def binaryName
    def updatedBinaryLocation
    def dimension
    def thumbnailFile

    // Select files having extension under the sourceBaseFolder.
    def files = importTask.findFilesByNamePattern(sourceBaseFolder, "^\\w+\\.\\w+\$" , 1, 10)
    def record

    files.eachWithIndex { file, i ->
      try {
        thumbnailFile = null

        // Determine the target binary handle node path to create or update.
        binaryLocation = targetBaseFolderNodePath + "/" + file.name.baseName

        // Record instance to store execution status and detail of a unit of migration work item.
        // these record instances will be collected and summarized when #logSummary() invoked later.
        record = importTask.beginRecord("", binaryLocation)
        record.setAttribute("file", file.name.path)

        // Find mimeType from configuration by extension. If not found, skip processing.
        mimeType = extensionMimeTypes.get(file.name.extension)

        if (StringUtils.isNotBlank(mimeType)) {
          record.setProcessed(true)

          // Find the image dimension of source image file.
          dimension = identifyDimension(file)

          // Create Hippo gallery imageset ContentNode.
          contentNode = new ContentNode(file.name.baseName, "hippogallery:imageset")
          contentNode.addMixinType("mix:referenceable")
          contentNode.setProperty("hippogallery:description", "Description for " + file.name.baseName)
          contentNode.setProperty("hippogallery:filename", file.name.baseName)

          // Create hippogallery:original child content node and add it to contentNode.
          originalNode = new ContentNode("hippogallery:original", "hippogallery:image")
          originalNode.setProperty("jcr:mimeType", mimeType)
          originalNode.setProperty("jcr:lastModified", ContentPropertyType.DATE, ISO8601.format(Calendar.getInstance()))
          originalNode.setProperty("hippogallery:width", ContentPropertyType.LONG, "" + dimension.width)
          originalNode.setProperty("hippogallery:height", ContentPropertyType.LONG, "" + dimension.height)
          originalNode.setProperty("jcr:data", ContentPropertyType.BINARY, file.getURL().toString())
          contentNode.addNode(originalNode)

          // Create a thumbnail image file (in JPEG) from the original and store it to a temporary file by using GraphicsMagickCommandUtils.
          thumbnailFile = ContentFileObjectUtils.createTempFile(file.name.baseName, ".jpg")
          resizeImage(file, thumbnailFile, ImageDimension.from("60x60"))
          // Find the image dimension of thumbnail image file.
          dimension = identifyDimension(thumbnailFile)

          // Create hippogallery:thumbnail child content node and add it to contentNode.
          thumbnailNode = new ContentNode("hippogallery:thumbnail", "hippogallery:image")
          thumbnailNode.setProperty("jcr:mimeType", "image/jpeg")
          thumbnailNode.setProperty("jcr:lastModified", ContentPropertyType.DATE, ISO8601.format(Calendar.getInstance()))
          thumbnailNode.setProperty("hippogallery:width", ContentPropertyType.LONG, "" + dimension.width)
          thumbnailNode.setProperty("hippogallery:height", ContentPropertyType.LONG, "" + dimension.height)
          thumbnailNode.setProperty("jcr:data", ContentPropertyType.BINARY, thumbnailFile.getURL().toString())
          contentNode.addNode(thumbnailNode)

          // Split target folder path and binary handle node name from the binaryLocation.
          def folderPathAndName = ContentPathUtils.splitToFolderPathAndName(binaryLocation)
          binaryFolderPath = folderPathAndName[0]
          binaryName = folderPathAndName[1]

          // Hippo Gallery image folder properties.
          binaryFolderPrimaryTypeName = "hippogallery:stdImageGallery"
          binaryFolderFolderTypes = [ "new-image-folder" ] as String[]
          binaryFolderGalleryTypes = [ "hippogallery:imageset" ] as String[]

          // Make sure that the binary target folder exists or created.
          binaryFolderPath =
              importTask.createOrUpdateBinaryFolder(binaryFolderPath, binaryFolderPrimaryTypeName,
                                                    binaryFolderFolderTypes, binaryFolderGalleryTypes)

          // Create or update binary content from contentNode.
          updatedBinaryLocation =
              importTask.createOrUpdateBinaryFromContentNode(contentNode, binaryPrimaryTypeName,
                                                           binaryFolderPath, binaryName)

          visitorContext.reportUpdated(binaryLocation)
          log.debug "Imported binary from '${file.name.path}' to '${updatedBinaryLocation}'."
          record.setSucceeded(true)
        }
      } catch (e) {
        log.error("Failed to process record.", e)
        visitorContext.reportFailed(binaryLocation)
        record.setErrorMessage(e.toString())
      } finally {
        if (thumbnailFile != null) {
          thumbnailFile.delete()
        }
        importTask.endRecord()
      }
    }

    return false
  }

  boolean undoUpdate(Node node) {
    throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
  }

  void destroy() {
    importTask.stop()
    importTask.logSummary()
  }

  ImageDimension identifyDimension(FileObject file) {
    // By default, use the pure-Java solution, ScalrProcessorUtils.
    // Optionally, use either GraphicsMagickCommandUtils or ImageMagickCommandUtils.
    def dimension
    if (imageProcessor == "org.onehippo.forge.gallerymagick.core.command.GraphicsMagickCommandUtils") {
      dimension = GraphicsMagickCommandUtils.identifyDimension(ContentFileObjectUtils.toFile(file))
    } else if (imageProcessor == "org.onehippo.forge.gallerymagick.core.command.ImageMagickCommandUtils") {
      dimension = ImageMagickCommandUtils.identifyDimension(ContentFileObjectUtils.toFile(file))
    } else {
      dimension = ScalrProcessorUtils.identifyDimension(ContentFileObjectUtils.toFile(file))
    }
    return dimension
  }

  void resizeImage(FileObject sourceFile, FileObject targetFile, ImageDimension dimension) {
    // By default, use the pure-Java solution, ScalrProcessorUtils.
    // Optionally, use either GraphicsMagickCommandUtils or ImageMagickCommandUtils.
    if (imageProcessor == "org.onehippo.forge.gallerymagick.core.command.GraphicsMagickCommandUtils") {
      GraphicsMagickCommandUtils.resizeImage(ContentFileObjectUtils.toFile(sourceFile), ContentFileObjectUtils.toFile(targetFile), dimension)
    } else if (imageProcessor == "org.onehippo.forge.gallerymagick.core.command.ImageMagickCommandUtils") {
      ImageMagickCommandUtils.resizeImage(ContentFileObjectUtils.toFile(sourceFile), ContentFileObjectUtils.toFile(targetFile), dimension)
    } else {
      ScalrProcessorUtils.resizeImage(ContentFileObjectUtils.toFile(sourceFile), ContentFileObjectUtils.toFile(targetFile), dimension)
    }
  }
}
