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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.onehippo.forge.content.pojo.model.ContentProperty;

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
        replaceDocbasesByPaths(session, baseContentNode, jxpath, null);
    }

    /**
     * Selects all the {@link ContentNode} objects under {@code baseContentNode}
     * by the given <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression, {@code jxpath}
     * and replace the <code>hippo:docbase</code> property value by the path of the JCR node found by the existing UUID string value,
     * and add those paths to {@code paths} collection.
     * {@code session} is used when finding a JCR node associated by the UUID value at the existing {@code hippo:docbase} property.
     * @param session JCR session
     * @param baseContentNode base {@link ContentNode} instance
     * @param jxpath <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression
     * @param paths replaced paths collection
     * @throws RepositoryException if fails to find a JCR node associated by the UUID value
     */
    public static void replaceDocbasesByPaths(final Session session, final ContentNode baseContentNode,
            final String jxpath, final Collection<String> paths) throws RepositoryException {
        List<ContentNode> mirrors = baseContentNode.queryNodesByXPath(jxpath);
        Node linkedNode;
        String linkedNodePath;

        for (ContentNode mirror : mirrors) {
            String docbase = mirror.getProperty("hippo:docbase").getValue();

            if (StringUtils.isNotBlank(docbase) && !StringUtils.equals(ROOT_NODE_UUID, docbase)) {
                try {
                    linkedNode = session.getNodeByIdentifier(docbase);
                    linkedNodePath = linkedNode.getPath();
                    mirror.setProperty("hippo:docbase", linkedNodePath);
                    if (paths != null) {
                        paths.add(linkedNodePath);
                    }
                } catch (ItemNotFoundException ignore) {
                }
            }
        }
    }

    /**
     * Selects all the {@link ContentProperty} objects under {@code baseContentNode}
     * by the given <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression, {@code jxpath}
     * and replace the string docbase property value by the path of the JCR node found by the existing UUID string value.
     * {@code session} is used when finding a JCR node associated by the UUID value at the existing string UUID property value.
     * @param session JCR session
     * @param baseContentNode base {@link ContentNode} instance
     * @param jxpath <a href="https://commons.apache.org/proper/commons-jxpath/">JXPath</a> expression
     * @throws RepositoryException if fails to find a JCR node associated by the UUID value
     */
    public static void replaceDocbasePropertiesByPaths(final Session session, final ContentNode baseContentNode, final String jxpath) throws RepositoryException {
        List<ContentProperty> docbaseProps = baseContentNode.queryPropertiesByXPath(jxpath);
        Node linkedNode;
        List<String> docbases;
        List<String> docpaths;

        for (ContentProperty docbaseProp : docbaseProps) {
            docbases = docbaseProp.getValues();

            if (CollectionUtils.isNotEmpty(docbases)) {
                docpaths = new LinkedList<>();

                for (String docbase : docbases) {
                    String docpath = docbase;

                    try {
                        linkedNode = session.getNodeByIdentifier(docbase);
                        docpath = linkedNode.getPath();
                    } catch (ItemNotFoundException ignore) {
                    } finally {
                        docpaths.add(docpath);
                    }
                }

                docbaseProp.removeValues();

                for (String docpath : docpaths) {
                    docbaseProp.addValue(docpath);
                }
            }
        }
    }

    /**
     * Find a given {@code urlPrefix} in the URL value of <code>jcr:data</code> property, and remove the prefix
     * if the value starts with the {@code urlPrefix}.
     * @param baseContentNode base content node
     * @param urlPrefix url prefix
     */
    public static void removeUrlPrefixInJcrDataValues(ContentNode baseContentNode, String urlPrefix) {
        final int urlPrefixLen = urlPrefix.length();
        List<ContentNode> childNodes = baseContentNode.queryNodesByXPath("//nodes[properties[@itemName='jcr:data']]");

        for (ContentNode childNode : childNodes) {
            String value = childNode.getProperty("jcr:data").getValue();

            if (StringUtils.startsWith(value, urlPrefix)) {
                childNode.setProperty("jcr:data", value.substring(urlPrefixLen));
            }
        }
    }

    /**
     * Find a given {@code urlPrefix} in the URL value of <code>jcr:data</code> property, and prepend it with the
     * given {@code urlPrefix} if the value starts with the {@code startsWith}.
     * @param baseContentNode base content node
     * @param startsWith start with string of the value
     * @param urlPrefix url prefix
     */
    public static void prependUrlPrefixInJcrDataValues(ContentNode baseContentNode, String startsWith, String urlPrefix) {
        List<ContentNode> childNodes = baseContentNode.queryNodesByXPath("//nodes[properties[@itemName='jcr:data']]");

        for (ContentNode childNode : childNodes) {
            String value = childNode.getProperty("jcr:data").getValue();

            if (StringUtils.startsWith(value, startsWith)) {
                childNode.setProperty("jcr:data", urlPrefix + value);
            }
        }
    }
}
