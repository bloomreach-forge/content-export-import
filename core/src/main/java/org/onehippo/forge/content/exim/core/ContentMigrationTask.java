/*
 * Copyright 2016-2024 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.core;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.onehippo.forge.content.pojo.model.ContentNode;
import org.slf4j.Logger;

/**
 * Content Migration (Export or Import) Task interface.
 */
public interface ContentMigrationTask {

    /**
     * Returns logger used by this task.
     * @return logger used by this task
     */
    public Logger getLogger();

    /**
     * Sets a logger to be used by this task.
     * @param logger logger to be used by this task
     */
    public void setLogger(Logger logger);

    /**
     * Starts this task. By 'starting', this task is supposed to reset its content migration records
     * and initialize the internal data. e.g, started time milliseconds.
     */
    public void start();

    /**
     * Stops this task. By 'stopping', this task is supposed to update the internal data. e.g, stopped time milliseconds.
     * But this task is supposed to keep the content migration records even after stopping for reporting purpose.
     */
    public void stop();

    /**
     * Returns the started time milliseconds.
     * @return the started time milliseconds
     */
    public long getStartedTimeMillis();

    /**
     * Returns the stopped time milliseconds.
     * @return the stopped time milliseconds
     */
    public long getStoppedTimeMillis();

    /**
     * Begins a new unit of content migration work item which can be identified by
     * either {@code contentId} or {@code contentPath}.
     * @param contentId content identifier for this unit of content migration work
     * @param contentPath content path for this unit of content migration work
     * @return a new {@link ContentMigrationRecord} instance
     */
    public ContentMigrationRecord beginRecord(String contentId, String contentPath);

    /**
     * Ends the current unit of content migration work item.
     * @return the current {@link ContentMigrationRecord} instance
     */
    public ContentMigrationRecord endRecord();

    /**
     * Returns the collection containing all the content migration work item records.
     * @return the collection containing all the content migration work item records
     */
    public Collection<ContentMigrationRecord> getContentMigrationRecords();

    /**
     * Return the execution summary.
     * @return the execution summary
     */
    public String getSummary();

    /**
     * Logs the execution summary by using the logger.
     */
    public void logSummary();

    /**
     * Reads {@code sourceFile} containing a {@link ContentNode} data in JSON format
     * and returns a parsed {@link ContentNode} object.
     * @param sourceFile source file containing a {@link ContentNode} data in JSON format
     * @return a parsed {@link ContentNode} object
     * @throws ContentMigrationException if reading fails.
     */
    public ContentNode readContentNodeFromJsonFile(FileObject sourceFile) throws ContentMigrationException;

    /**
     * Writes {@code contentNode} object into {@code targetFile} in JSON format.
     * @param contentNode a {@link ContentNode} object
     * @param targetFile target file to write the {@code contentNode}
     * @throws ContentMigrationException if writing fails.
     */
    public void writeContentNodeToJsonFile(ContentNode contentNode, FileObject targetFile)
            throws ContentMigrationException;

    /**
     * Reads {@code sourceFile} containing a {@link ContentNode} data in XML format
     * and returns a parsed {@link ContentNode} object.
     * @param sourceFile source file containing a {@link ContentNode} data in XML format
     * @return a parsed {@link ContentNode} object
     * @throws ContentMigrationException if reading fails.
     */
    public ContentNode readContentNodeFromXmlFile(FileObject sourceFile) throws ContentMigrationException;

    /**
     * Writes {@code contentNode} object into {@code targetFile} in XML format.
     * @param contentNode a {@link ContentNode} object
     * @param targetFile target file to write the {@code contentNode}
     * @throws ContentMigrationException if writing fails.
     */
    public void writeContentNodeToXmlFile(ContentNode contentNode, FileObject targetFile)
            throws ContentMigrationException;

}
