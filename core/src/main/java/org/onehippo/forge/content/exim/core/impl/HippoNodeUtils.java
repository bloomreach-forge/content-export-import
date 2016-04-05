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
package org.onehippo.forge.content.exim.core.impl;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hippo specific node related utilities.
 */
class HippoNodeUtils {

    private static Logger log = LoggerFactory.getLogger(HippoNodeUtils.class);

    /**
     * Hippo Repository specific predefined folder node type name
     */
    private static final String DEFAULT_HIPPO_FOLDER_NODE_TYPE = "hippostd:folder";

    /**
     * The workflow category name to get a folder workflow. We use threepane as this is the same as the CMS uses
     */
    private static final String DEFAULT_HIPPO_FOLDER_WORKFLOW_CATEGORY = "threepane";

    /**
     * The workflow category name to add a new document.
     */
    private static final String DEFAULT_NEW_DOCUMENT_WORKFLOW_CATEGORY = "new-document";

    /**
     * The workflow category name to add a new folder.
     */
    private static final String DEFAULT_NEW_FOLDER_WORKFLOW_CATEGORY = "new-folder";

    /**
     * The workflow category name to localize the new document
     */
    private static final String DEFAULT_WORKFLOW_CATEGORY = "core";

    /**
     * The codec which is used for the node names
     */
    static final StringCodec DEFAULT_URI_ENCODING = new StringCodecFactory.UriEncoding();

    private HippoNodeUtils() {
    }

    /**
     * Finds a child node by {@code childNodeName} and {@code childNodeTypes} under the {@code baseNode}.
     * @param baseNode base node
     * @param childNodeName child node name
     * @param childNodeTypes child node type names
     * @return a child node by {@code childNodeName} and {@code childNodeTypes} under the {@code baseNode}
     * @throws RepositoryException if any repository/workflow exception occurs
     */
    public static Node getChildNodeOfType(final Node baseNode, final String childNodeName, final String ... childNodeTypes)
            throws RepositoryException {
        if (!baseNode.hasNode(childNodeName)) {
            return null;
        }

        Node childNode;

        for (NodeIterator nodeIt = baseNode.getNodes(childNodeName); nodeIt.hasNext(); ) {
            childNode = nodeIt.nextNode();

            if (StringUtils.equals(childNodeName, childNode.getName())) {
                if (childNodeTypes == null || childNodeTypes.length == 0) {
                    return childNode;
                } else {
                    for (String childNodeType : childNodeTypes) {
                        if (childNode.isNodeType(childNodeType)) {
                            return childNode;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns {@link Workflow} instance by the {@code category} for the {@code node}.
     * @param session JCR session
     * @param category workflow category
     * @param node folder or document node
     * @return {@link Workflow} instance for the {@code node} and the {@code category}
     * @throws RepositoryException if any repository/workflow exception occurs
     */
    public static Workflow getHippoWorkflow(final Session session, final String category, final Node node)
            throws RepositoryException {
        Workspace workspace = session.getWorkspace();

        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }

            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();

            return wfm.getWorkflow(category, node);
        } finally {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }

    /**
     * Returns a map of variant nodes, keyed by variant states such as {@link HippoStdNodeType.PUBLISHED} or {@link HippoStdNodeType.UNPUBLISHED}.
     * @param handle document handle node
     * @return a map of variant nodes, keyed by variant states such as {@link HippoStdNodeType.PUBLISHED} or {@link HippoStdNodeType.UNPUBLISHED}
     * @throws RepositoryException if any repository/workflow exception occurs
     */
    public static Map<String, Node> getDocumentVariantsMap(final Node handle) throws RepositoryException {
        Map<String, Node> variantsMap = new HashMap<>();
        Node variantNode = null;
        String hippoState;

        for (NodeIterator nodeIt = handle.getNodes(handle.getName()); nodeIt.hasNext(); ) {
            variantNode = nodeIt.nextNode();

            if (variantNode.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                hippoState = variantNode.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                variantsMap.put(hippoState, variantNode);
            }
        }

        return variantsMap;
    }

    /**
     * Checks if all the folders exist in the given {@code absPath} and creates folders if not existing.
     * @param session JCR session
     * @param absPath absolute folder node path
     * @return the final folder node if successful
     * @throws RepositoryException if any repository exception occurs
     * @throws WorkflowException if any workflow exception occurs
     */
    public static Node createMissingHippoFolders(final Session session, String absPath)
            throws RepositoryException, WorkflowException {
        String[] folderNames = StringUtils.split(absPath, "/");

        Node rootNode = session.getRootNode();
        Node curNode = rootNode;
        String folderNodePath;

        for (String folderName : folderNames) {
            String folderNodeName = DEFAULT_URI_ENCODING.encode(folderName);

            if (curNode == rootNode) {
                folderNodePath = "/" + folderNodeName;
            } else {
                folderNodePath = curNode.getPath() + "/" + folderNodeName;
            }

            Node existingFolderNode = getExistingHippoFolderNode(session, folderNodePath);

            if (existingFolderNode == null) {
                curNode = session
                        .getNode(createHippoFolderNodeByWorkflow(session, curNode, DEFAULT_HIPPO_FOLDER_NODE_TYPE, folderName));
            } else {
                curNode = existingFolderNode;
            }

            curNode = getHippoCanonicalNode(curNode);

            if (isHippoMirrorNode(curNode)) {
                curNode = getRereferencedNodeByHippoMirror(curNode);
            }
        }

        return curNode;
    }

    /**
     * Returns {@code node} if it is a document handle node or its parent if it is a document variant node.
     * Otherwise returns null.
     * @param node JCR node
     * @return {@code node} if it is a document handle node or its parent if it is a document variant node. Otherwise returns null.
     * @throws RepositoryException if repository exception occurs
     */
    public static Node getHippoDocumentHandle(Node node) throws RepositoryException {
        if (node.isNodeType("hippo:handle")) {
            return node;
        } else if (node.isNodeType("hippo:document")) {
            if (!node.getSession().getRootNode().isSame(node)) {
                Node parentNode = node.getParent();

                if (parentNode.isNodeType("hippo:handle")) {
                    return parentNode;
                }
            }
        }

        return null;
    }

    /**
     * Finds and returns the canonical node from the {@code node}.
     * @param node node
     * @return the canonical node from the {@code node}
     */
    static Node getHippoCanonicalNode(Node node) {
        if (node instanceof HippoNode) {
            HippoNode hnode = (HippoNode) node;

            try {
                Node canonical = hnode.getCanonicalNode();

                if (canonical == null) {
                    log.debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the "
                            + "virtual node. Return null", node.getPath());
                }

                return canonical;
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching canonical node. Return null", e);
                throw new RuntimeException(e);
            }
        }

        return node;
    }

    /**
     * Returns true if the {@code node} is a either <code>hippo:facetselect</code> or <code>hippo:mirror</code> node.
     * @param node node
     * @return true if the {@code node} is a either <code>hippo:facetselect</code> or <code>hippo:mirror</code> node
     * @throws RepositoryException if unexpected repository exception occurs
     */
    static boolean isHippoMirrorNode(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_FACETSELECT) || node.isNodeType(HippoNodeType.NT_MIRROR)) {
            return true;
        }

        return false;
    }

    /**
     * Returns the referenced node by the given mirror node ({@code mirrorNode}).
     * @param mirrorNode mirror node
     * @return the referenced node by the given mirror node ({@code mirrorNode})
     */
    static Node getRereferencedNodeByHippoMirror(Node mirrorNode) {
        String docBaseUUID = null;

        try {
            if (!isHippoMirrorNode(mirrorNode)) {
                log.info("Cannot deref a node that is not of (sub)type '{}' or '{}'. Return null",
                        HippoNodeType.NT_FACETSELECT, HippoNodeType.NT_MIRROR);
                return null;
            }

            // HippoNodeType.HIPPO_DOCBASE is a mandatory property so no need to test if exists
            docBaseUUID = mirrorNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();

            try {
                return mirrorNode.getSession().getNodeByIdentifier(docBaseUUID);
            } catch (IllegalArgumentException e) {
                log.warn("Docbase cannot be parsed to a valid uuid. Return null");
                return null;
            }
        } catch (ItemNotFoundException e) {
            String path = null;

            try {
                path = mirrorNode.getPath();
            } catch (RepositoryException e1) {
                log.error("RepositoryException, cannot return deferenced node: {}", e1);
            }

            log.info(
                    "ItemNotFoundException, cannot return deferenced node because docbase uuid '{}' cannot be found. The docbase property is at '{}/hippo:docbase'. Return null",
                    docBaseUUID, path);
        } catch (RepositoryException e) {
            log.error("RepositoryException, cannot return deferenced node: {}", e);
        }

        return null;
    }

    /**
     * Finds and returns a hippo folder node located at {@code absPath} if found. If not found, returns null.
     * @param session JCR session
     * @param absPath absolute folder node path
     * @return a hippo folder node located at {@code absPath} if found. If not found, returns null
     * @throws RepositoryException if unexpected repository exception occurs
     */
    static Node getExistingHippoFolderNode(final Session session, final String absPath)
            throws RepositoryException {
        if (!session.nodeExists(absPath)) {
            return null;
        }

        Node node = session.getNode(absPath);
        Node candidateNode = null;

        if (session.getRootNode().isSame(node)) {
            return session.getRootNode();
        } else {
            Node parentNode = node.getParent();
            for (NodeIterator nodeIt = parentNode.getNodes(node.getName()); nodeIt.hasNext();) {
                Node siblingNode = nodeIt.nextNode();
                if (!isHippoDocumentHandleOrVariant(siblingNode)) {
                    candidateNode = siblingNode;
                    break;
                }
            }
        }

        if (candidateNode == null) {
            return null;
        }

        Node canonicalFolderNode = getHippoCanonicalNode(candidateNode);

        if (isHippoMirrorNode(canonicalFolderNode)) {
            canonicalFolderNode = getRereferencedNodeByHippoMirror(canonicalFolderNode);
        }

        if (canonicalFolderNode == null) {
            return null;
        }

        if (isHippoDocumentHandleOrVariant(canonicalFolderNode)) {
            return null;
        }

        return canonicalFolderNode;
    }

    /**
     * Returns true if {@code node} is either document handle node or document variant node.
     * @param node node
     * @return true if {@code node} is either document handle node or document variant node
     * @throws RepositoryException
     */
    static boolean isHippoDocumentHandleOrVariant(Node node) throws RepositoryException {
        if (node.isNodeType("hippo:handle")) {
            return true;
        } else if (node.isNodeType("hippo:document")) {
            if (!node.getSession().getRootNode().isSame(node)) {
                Node parentNode = node.getParent();

                if (parentNode.isNodeType("hippo:handle")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a hippo folder by using Hippo Repository Workflow.
     * @param session JCR session
     * @param folderNode base folder node
     * @param nodeTypeName folder node type name
     * @param name folder node name
     * @return absolute path of the created folder
     * @throws RepositoryException if unexpected repository exception occurs
     * @throws WorkflowException if unexpected workflow exception occurs
     */
    static String createHippoFolderNodeByWorkflow(final Session session, Node folderNode, String nodeTypeName,
            String name) throws RepositoryException, WorkflowException {
        try {
            folderNode = getHippoCanonicalNode(folderNode);
            Workflow wf = getHippoWorkflow(session, DEFAULT_HIPPO_FOLDER_WORKFLOW_CATEGORY, folderNode);

            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;

                String category = DEFAULT_NEW_DOCUMENT_WORKFLOW_CATEGORY;

                if (nodeTypeName.equals(DEFAULT_HIPPO_FOLDER_NODE_TYPE)) {
                    category = DEFAULT_NEW_FOLDER_WORKFLOW_CATEGORY;

                    // now check if there is some more specific workflow for hippostd:folder
                    if (fwf.hints() != null && fwf.hints().get("prototypes") != null) {
                        Object protypesMap = fwf.hints().get("prototypes");
                        if (protypesMap instanceof Map) {
                            for (Object o : ((Map) protypesMap).entrySet()) {
                                Entry entry = (Entry) o;
                                if (entry.getKey() instanceof String && entry.getValue() instanceof Set) {
                                    if (((Set) entry.getValue()).contains(DEFAULT_HIPPO_FOLDER_NODE_TYPE)) {
                                        // we found possibly a more specific workflow for folderNodeTypeName. Use the key as category
                                        category = (String) entry.getKey();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                String nodeName = DEFAULT_URI_ENCODING.encode(name);
                String added = fwf.add(category, nodeTypeName, nodeName);
                if (added == null) {
                    throw new WorkflowException("Failed to add document/folder for type '" + nodeTypeName
                            + "'. Make sure there is a prototype.");
                }
                Node addedNode = folderNode.getSession().getNode(added);
                if (!nodeName.equals(name)) {
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getHippoWorkflow(session, DEFAULT_WORKFLOW_CATEGORY,
                            addedNode);
                    defaultWorkflow.localizeName(name);
                }

                if (DEFAULT_NEW_DOCUMENT_WORKFLOW_CATEGORY.equals(category)) {

                    // added new document : because the document must be in 'preview' availability, we now set this explicitly
                    if (addedNode.isNodeType("hippostd:publishable")) {
                        log.info("Added document '{}' is pusblishable so set status to preview.", addedNode.getPath());
                        addedNode.setProperty("hippostd:state", "unpublished");
                        addedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[] { "preview" });
                    } else {
                        log.info("Added document '{}' is not publishable so set status to live & preview directly.",
                                addedNode.getPath());
                        addedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[] { "live", "preview" });
                    }

                    if (addedNode.isNodeType("hippostd:publishableSummary")) {
                        addedNode.setProperty("hippostd:stateSummary", "new");
                    }
                    addedNode.getSession().save();
                }
                return added;
            } else {
                throw new WorkflowException(
                        "Can't create folder " + name + " [" + nodeTypeName + "] in the folder " + folderNode.getPath()
                                + ", because there is no FolderWorkflow possible on the folder node: " + wf);
            }
        } catch (RemoteException e) {
            throw new WorkflowException(e.toString(), e);
        }
    }
}