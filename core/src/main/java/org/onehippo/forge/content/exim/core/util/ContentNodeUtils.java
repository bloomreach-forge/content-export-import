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

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.content.pojo.model.ContentNode;

/**
 * Utilities to handle {@link ContentNode} objects.
 */
public class ContentNodeUtils {

    /**
     * Default <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression
     * to select all the nodes having <code>hippo:docbase</code> property.
     */
    public static final String MIRROR_DOCBASES_XPATH = "//nodes[properties[@itemName='hippo:docbase']]";

    /**
     * The ROOT node identifier of Jackrabbit Repository.
     */
    private static final String ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    private ContentNodeUtils() {
    }

    /**
     * Selects all the {@link ContentNode} objects under {@code baseContentNode}
     * by the default <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression, {@link #MIRROR_DOCBASES_XPATH}
     * and replace the <code>hippo:docbase</code> property value by the path of the JCR node found by the existing UUID string value.
     * {@code session} is used when finding a JCR node associated by the UUID value at the existing {@code hippo:docbase} property.
     * @param session JCR session
     * @param baseContentNode base {@link ContentNode} instance
     * @throws RepositoryException if fails to find a JCR node associated by the UUID value
     */
    public static void replaceDocbasesByPaths(final Session session, final ContentNode baseContentNode) throws RepositoryException {
        replaceDocbasesByPaths(session, baseContentNode, MIRROR_DOCBASES_XPATH);
    }

    /**
     * Selects all the {@link ContentNode} objects under {@code baseContentNode}
     * by the given <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression, {@code jxpath}
     * and replace the <code>hippo:docbase</code> property value by the path of the JCR node found by the existing UUID string value.
     * {@code session} is used when finding a JCR node associated by the UUID value at the existing {@code hippo:docbase} property.
     * @param session JCR session
     * @param baseContentNode base {@link ContentNode} instance
     * @param jxpath <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression
     * @throws RepositoryException if fails to find a JCR node associated by the UUID value
     */
    public static void replaceDocbasesByPaths(final Session session, final ContentNode baseContentNode, final String jxpath) throws RepositoryException {
        List<ContentNode> mirrors = baseContentNode.queryNodesByXPath(jxpath);
        Node linkedNode;

        for (ContentNode mirror : mirrors) {
            String docbase = mirror.getProperty("hippo:docbase").getValue();

            if (StringUtils.isNotBlank(docbase) && !StringUtils.equals(ROOT_NODE_UUID, docbase)) {
                try {
                    linkedNode = session.getNodeByIdentifier(docbase);
                    mirror.setProperty("hippo:docbase", linkedNode.getPath());
                } catch (ItemNotFoundException ignore) {
                }
            }
        }
    }

}
