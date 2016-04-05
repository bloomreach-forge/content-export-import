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

public class ContentNodeUtils {

    public static final String MIRROR_DOCBASES_XPATH = "//nodes[properties[@itemName='hippo:docbase']]";

    private static final String ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    private ContentNodeUtils() {
    }

    public static void replaceDocbasesByPaths(final Session session, final ContentNode baseContentNode) throws RepositoryException {
        replaceDocbasesByPaths(session, baseContentNode, MIRROR_DOCBASES_XPATH);
    }

    public static void replaceDocbasesByPaths(final Session session, final ContentNode baseContentNode, final String xpath) throws RepositoryException {
        List<ContentNode> mirrors = baseContentNode.queryNodesByXPath(xpath);
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
