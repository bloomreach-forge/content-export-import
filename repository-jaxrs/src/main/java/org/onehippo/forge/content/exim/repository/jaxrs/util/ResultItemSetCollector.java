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
package org.onehippo.forge.content.exim.repository.jaxrs.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.content.exim.core.util.HippoNodeUtils;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ExecutionParams;
import org.onehippo.forge.content.exim.repository.jaxrs.param.QueriesAndPaths;
import org.onehippo.forge.content.exim.repository.jaxrs.param.Result;
import org.onehippo.forge.content.exim.repository.jaxrs.param.ResultItem;

/**
 * Utility to collect {@link ResultItem}s based on various conditions.
 */
public class ResultItemSetCollector {

    private ResultItemSetCollector() {
    }

    /**
     * Collect {@link ResultItem}s from the given {@code params} by picking nodes from the given paths or querying
     * nodes from the given queries.
     * @param session JCR session
     * @param params ExecutionParams instance
     * @return collected {@link ResultItem}s
     * @throws RepositoryException if repository exception occurs
     */
    public static Result collectItemsFromExecutionParams(final Session session, final ExecutionParams params)
            throws RepositoryException {
        Result result = new Result();

        QueriesAndPaths binaries = params.getBinaries();

        if (binaries != null) {
            Set<String> binaryPathsCache = new LinkedHashSet<>();
            fillResultItemsForNodePaths(session, binaries.getPaths(), true, binaryPathsCache, result);
            fillResultItemsFromQueries(session, binaries.getQueries(), true, binaryPathsCache, result);
        }

        QueriesAndPaths documents = params.getDocuments();

        if (documents != null) {
            Set<String> documentPathsCache = new LinkedHashSet<>();
            fillResultItemsForNodePaths(session, documents.getPaths(), false, documentPathsCache, result);
            fillResultItemsFromQueries(session, documents.getQueries(), false, documentPathsCache, result);
        }

        return result;
    }

    /**
     * Collect nodes from {@code nodePaths} with validations and fill {@link ResultItem} instances in {@code resultOut}.
     * @param session JCR session
     * @param nodePaths document or binary node paths to validate
     * @param binary flag whether the node paths are for binary content or not
     * @param pathsCache node path cache set, which can be useful if you want to avoid putting the same items multiple times.
     *                   This can be null.
     * @param resultOut {@link Result} instance
     * @throws RepositoryException if repository exception occurs
     */
    public static void fillResultItemsForNodePaths(Session session, Collection<String> nodePaths,
            boolean binary, Set<String> pathsCache, Result resultOut) throws RepositoryException {
        if (pathsCache == null) {
            pathsCache = new HashSet<>();
        }

        for (String path : nodePaths) {
            if ((binary && !HippoNodeUtils.isBinaryPath(path)) || (!binary && !HippoNodeUtils.isDocumentPath(path))) {
                continue;
            }

            if (!session.nodeExists(path)) {
                continue;
            }

            Node handle = HippoNodeUtils.getHippoDocumentHandle(session.getNode(path));

            if (handle == null) {
                continue;
            }

            String handlePath = handle.getPath();

            if (pathsCache.contains(handlePath)) {
                continue;
            }

            Node firstVariant = HippoNodeUtils.getFirstVariantNode(handle);

            if (firstVariant == null) {
                continue;
            }

            pathsCache.add(handlePath);
            ResultItem item = new ResultItem(handlePath, firstVariant.getPrimaryNodeType().getName());
            resultOut.addItem(item);
        }
    }

    /**
     * Collect nodes by executing the {@code queries} with validations and fill {@link ResultItem} instances in
     * {@code resultOut}.
     * @param session JCR session
     * @param queries JCR query statements for documents or binaries
     * @param binary flag whether the node paths are for binary content or not
     * @param pathsCache node path cache set, which can be useful if you want to avoid putting the same items multiple times.
     *                   This can be null.
     * @param resultOut {@link Result} instance
     * @throws RepositoryException if repository exception occurs
     */
    public static void fillResultItemsFromQueries(Session session, Collection<String> queries,
            boolean binary, Set<String> pathsCache, Result resultOut) throws RepositoryException {
        for (String query : queries) {
            if (StringUtils.isBlank(query)) {
                continue;
            }

            if (!StringUtils.startsWith(query, "/") || StringUtils.startsWithIgnoreCase(query, "select")) {
                continue;
            }

            final String language = (StringUtils.startsWithIgnoreCase(query, "select")) ? Query.SQL : Query.XPATH;
            Query jcrQuery = session.getWorkspace().getQueryManager().createQuery(query, language);
            QueryResult queryResult = jcrQuery.execute();

            for (NodeIterator nodeIt = queryResult.getNodes(); nodeIt.hasNext();) {
                Node node = nodeIt.nextNode();

                if (node == null) {
                    continue;
                }

                String nodePath = node.getPath();

                if ((binary && !HippoNodeUtils.isBinaryPath(nodePath)) || (!binary && !HippoNodeUtils.isDocumentPath(nodePath))) {
                    continue;
                }

                Node handle = HippoNodeUtils.getHippoDocumentHandle(node);

                if (handle == null) {
                    continue;
                }

                String handlePath = handle.getPath();

                if (pathsCache.contains(handlePath)) {
                    continue;
                }

                Node firstVariant = HippoNodeUtils.getFirstVariantNode(handle);

                if (firstVariant == null) {
                    continue;
                }

                pathsCache.add(handlePath);
                ResultItem item = new ResultItem(handlePath, firstVariant.getPrimaryNodeType().getName());
                resultOut.addItem(item);
            }
        }
    }
}
