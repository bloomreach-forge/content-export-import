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

import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.onehippo.cms7.essentials.components.model.AuthorEntry;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "contenteximdemo:author")
@Node(jcrType = "contenteximdemo:author")
public class Author extends HippoDocument implements AuthorEntry {

    public static final String ROLE = "contenteximdemo:role";
    public static final String ACCOUNTS = "contenteximdemo:accounts";
    public static final String FULL_NAME = "contenteximdemo:fullname";
    public static final String IMAGE = "contenteximdemo:image";
    public static final String CONTENT = "contenteximdemo:content";

    @HippoEssentialsGenerated(internalName = "contenteximdemo:fullname")
    public String getFullName() {
        return  getSingleProperty(FULL_NAME);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:content")
    public HippoHtml getContent() {
        return getHippoHtml(CONTENT);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:role")
    public String getRole() {
        return getSingleProperty(ROLE);
    }

    @HippoEssentialsGenerated(internalName = "contenteximdemo:image")
    public HippoGalleryImageSet getImage() {
        return getLinkedBean(IMAGE, HippoGalleryImageSet.class);
    }

  	@HippoEssentialsGenerated(internalName = "contenteximdemo:accounts")
	  public List<Account> getAccounts() {
		    return getChildBeansByName(ACCOUNTS, Account.class);
	  }
}
