/*
 * Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
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
     * @param folderType folderType
     * @param galleryType galleryType
     * @return the final folder node if successful
     * @throws RepositoryException if any repository exception occurs
     * @throws WorkflowException if any workflow exception occurs
     */
    public static Node createMissingHippoBinaryFolders(final Session session, String absPath, String primaryTypeName,
            String [] folderTypes, String [] galleryTypes) throws RepositoryException, WorkflowException {
        String[] folderNames = StringUtils.split(absPath, "/");

        Node rootNode = session.getRootNode();
        Node curNode = rootNode;
        Node tempFolderNode;
        String folderNodePath;

        for (String folderName : folderNames) {
            String folderNodeName = HippoNodeUtils.getDefaultUriEncoding().encode(folderName);

            if (curNode == rootNode) {
                folderNodePath = "/" + folderNodeName;
            } else {
                folderNodePath = curNode.getPath() + "/" + folderNodeName;
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

}