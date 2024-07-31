package org.onehippo.forge.content.exim.demo.beans;
/*
 * Copyright 2014-2024 Bloomreach B.V. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "contenteximdemo:newsdocument")
@Node(jcrType="contenteximdemo:newsdocument")
public class NewsDocument extends HippoDocument {

    /**
     * The document type of the news document.
     */
    public static final String DOCUMENT_TYPE = "contenteximdemo:newsdocument";

    private static final String TITLE = "contenteximdemo:title";
    private static final String DATE = "contenteximdemo:date";
    private static final String INTRODUCTION = "contenteximdemo:introduction";
    private static final String IMAGE = "contenteximdemo:image";
    private static final String CONTENT = "contenteximdemo:content";
    private static final String LOCATION = "contenteximdemo:location";
    private static final String AUTHOR = "contenteximdemo:author";
    private static final String SOURCE = "contenteximdemo:source";

    /**
     * Get the title of the document.
     *
     * @return the title
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:title")
    public String getTitle() {
        return getSingleProperty(TITLE);
    }

    /**
     * Get the date of the document.
     *
     * @return the date
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:date")
    public Calendar getDate() {
        return getSingleProperty(DATE);
    }

    /**
     * Get the introduction of the document.
     *
     * @return the introduction
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:introduction")
    public String getIntroduction() {
        return getSingleProperty(INTRODUCTION);
    }

    /**
     * Get the image of the document.
     *
     * @return the image
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:image")
    public HippoGalleryImageSet getImage() {
        return getLinkedBean(IMAGE, HippoGalleryImageSet.class);
    }

    /**
     * Get the main content of the document.
     *
     * @return the content
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:content")
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    /**
     * Get the location of the document.
     *
     * @return the location
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:location")
    public String getLocation() {
        return getSingleProperty(LOCATION);
    }

    /**
     * Get the author of the document.
     *
     * @return the author
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:author")
    public String getAuthor() {
        return getSingleProperty(AUTHOR);
    }

    /**
     * Get the source of the document.
     *
     * @return the source
     */
    @HippoEssentialsGenerated(internalName = "contenteximdemo:source")
    public String getSource() {
        return getSingleProperty(SOURCE);
    }

}

