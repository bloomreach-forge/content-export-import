/*
 * Copyright 2016-2022 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;

/**
 * Internal utility for Hippo binary related nodes.
 */
public class HippoBinaryNodeUtils {

    private HippoBinaryNodeUtils() {
    }

    /**
     * Checks if all the binary folders exist in the given {@code absPath} and creates binary folders if not existing.
     * @param session JCR session
     * @param absPath absolute binary folder node path
     * @param primaryTypeName primary folder node type name
     * @param folderTypes folderTypes
     * @param galleryTypes galleryTypes
     * @return the final folder node if successful
     * @throws RepositoryException if any repository exception occurs
     * @throws WorkflowException if any workflow exception occurs
     */
    public static Node createMissingHippoBinaryFolders(final Session session, String absPath, String primaryTypeName,
            String [] folderTypes, String [] galleryTypes) throws RepositoryException, WorkflowException {
        String[] folderNames = StringUtils.split(ContentPathUtils.encodeNodePath(ContentPathUtils.removeIndexNotationInNodePath(absPath)), "/");

        Node rootNode = session.getRootNode();
        Node curNode = rootNode;
        Node tempFolderNode;
        String folderNodePath;

        for (String folderName : folderNames) {

            if (curNode == rootNode) {
                folderNodePath = "/" + folderName;
            } else {
                folderNodePath = curNode.getPath() + "/" + folderName;
            }

            Node existingFolderNode = HippoNodeUtils.getExistingHippoFolderNode(session, folderNodePath);

            if (existingFolderNode == null) {
                tempFolderNode = curNode.addNode(folderName, primaryTypeName);
                tempFolderNode.addMixin("mix:referenceable");

                if (folderTypes != null) {
                    tempFolderNode.setProperty("hippostd:foldertype", folderTypes);
                }

                if (galleryTypes != null) {
                    tempFolderNode.setProperty("hippostd:gallerytype", galleryTypes);
                }

                curNode = tempFolderNode;
            } else {
                curNode = existingFolderNode;
            }

            curNode = HippoNodeUtils.getHippoCanonicalNode(curNode);

            if (HippoNodeUtils.isHippoMirrorNode(curNode)) {
                curNode = HippoNodeUtils.getRereferencedNodeByHippoMirror(curNode);
            }
        }

        return curNode;
    }

    /**
     * Finds binary resource node ({@code hippo:resource}) under the {@code handlePath}, extracts text content
     * and saves {@code hippo:text} property if the binary data is {@code application/pdf} content.
     * @param session JCR session
     * @param handlePath binary handle node path
     * @throws RepositoryException if repository exception occurs
     * @throws IOException if IO exception occurs
     * @throws TikaException if TIKA exception occurs
     */
    public static void extractTextFromBinariesAndSaveHippoTextsUnderHandlePath(final Session session, final String handlePath)
            throws RepositoryException, IOException, TikaException {
        if (StringUtils.isBlank(handlePath)) {
            return;
        }

        if (!session.nodeExists(handlePath)) {
            return;
        }

        extractTextFromBinariesAndSaveHippoTexts(session, session.getNode(handlePath));
    }

    /**
     * Finds binary resource node ({@code hippo:resource}) under the {@code handle}, extracts text content
     * and saves {@code hippo:text} property if the binary data is {@code application/pdf} content.
     * @param session JCR session
     * @param handle binary handle node
     * @throws RepositoryException if repository exception occurs
     * @throws IOException if IO exception occurs
     * @throws TikaException if TIKA exception occurs
     */
    public static void extractTextFromBinariesAndSaveHippoTexts(final Session session, final Node handle)
            throws RepositoryException, IOException, TikaException {
        List<Node> resourceNodes = new ArrayList<>();

        if (handle.isNodeType(HippoNodeType.NT_RESOURCE)) {
            resourceNodes.add(handle);
        } else if (handle.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            Node resourceNode;
            for (NodeIterator nodeIt = handle.getNodes(); nodeIt.hasNext(); ) {
                resourceNode = nodeIt.nextNode();
                if (resourceNode != null && resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                    resourceNodes.add(resourceNode);
                }
            }
        } else if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            Node assetSetNode;
            Node resourceNode;
            for (NodeIterator nodeIt1 = handle.getNodes(handle.getName()); nodeIt1.hasNext(); ) {
                assetSetNode = nodeIt1.nextNode();
                if (assetSetNode != null) {
                    for (NodeIterator nodeIt2 = assetSetNode.getNodes(); nodeIt2.hasNext(); ) {
                        resourceNode = nodeIt2.nextNode();
                        if (resourceNode != null && resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                            resourceNodes.add(resourceNode);
                        }
                    }
                }
            }
        }

        String mimeType = null;
        InputStream dataInput = null;
        String textContent = null;
        InputStream textInput = null;
        Binary textBinary = null;

        for (Node resourceNode : resourceNodes) {
            mimeType = (resourceNode.hasProperty("jcr:mimeType")) ? resourceNode.getProperty("jcr:mimeType").getString()
                    : null;

            if (!StringUtils.equals("application/pdf", mimeType)) {
                continue;
            }

            try {
                dataInput = resourceNode.getProperty("jcr:data").getBinary().getStream();
                textContent = TikaUtils.parsePdfToString(dataInput);
                textInput = new ByteArrayInputStream(textContent.getBytes());
                textBinary = session.getValueFactory().createBinary(textInput);
                resourceNode.setProperty(HippoNodeType.HIPPO_TEXT, textBinary);
            } finally {
                IOUtils.closeQuietly(textInput);
            }
        }
    }

}