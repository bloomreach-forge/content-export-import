package org.onehippo.forge.content.exim.demo.beans;
/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.onehippo.cms7.essentials.components.model.Authors;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "contenteximdemo:blogpost")
@Node(jcrType = "contenteximdemo:blogpost")
public class Blogpost extends HippoDocument implements Authors {

    public static final String TITLE = "contenteximdemo:title";
    public static final String INTRODUCTION = "contenteximdemo:introduction";
    public static final String CONTENT = "contenteximdemo:content";
    public static final String PUBLICATION_DATE = "contenteximdemo:publicationdate";
    public static final String CATEGORIES = "contenteximdemo:categories";
    public static final String AUTHOR = "contenteximdemo:author";
    public static final String AUTHOR_NAMES = "contenteximdemo:authornames";
    public static final String LINK = "contenteximdemo:link";
    public static final String AUTHORS = "contenteximdemo:authors";
    public static final String TAGS = "hippostd:tags";


   @HippoEssentialsGenerated(internalName = "contenteximdemo:publicationdate")
    public Calendar getPublicationDate() {
        return getSingleProperty(PUBLICATION_DATE);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:authornames")
    public String[] getAuthorNames() {
        return getMultipleProperty(AUTHOR_NAMES);
    }

    public String getAuthor() {
        final String[] authorNames = getAuthorNames();
        if(authorNames !=null && authorNames.length > 0){
            return authorNames[0];
        }
        return null;
    }


    @HippoEssentialsGenerated(internalName = "contenteximdemo:title")
    public String getTitle() {
        return getSingleProperty(TITLE);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:content")
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:introduction")
    public String getIntroduction() {
        return getSingleProperty(INTRODUCTION);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:categories")
    public String[] getCategories() {
        return getMultipleProperty(CATEGORIES);
    }

    @Override
    @HippoEssentialsGenerated(internalName = "contenteximdemo:authors")
    public List<Author> getAuthors() {
        return getLinkedBeans(AUTHORS, Author.class);
    }
}