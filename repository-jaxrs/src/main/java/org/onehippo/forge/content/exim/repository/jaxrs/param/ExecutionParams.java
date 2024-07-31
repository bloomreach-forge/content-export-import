/*
 * Copyright 2024 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.repository.jaxrs.param;

import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Content Export or Import Execution Parameters.
 */
public class ExecutionParams {

    /**
     * Default batch size.
     */
    private static final int DEFAULT_BATCH_SIZE = 200;

    /**
     * Default throttle time milliseconds on each batch execution cycle.
     */
    private static final long DEFAULT_THROTTLE = 10L;

    /**
     * Default binary data byte maximum size in DATA URLs.
     */
    private static final long DEFAULT_DATA_URL_SIZE_THRESHOLD = 256 * 1024;

    /**
     * An option of {@link #publishOnImport} value, not to publish a document automatically on import.
     */
    public static final String PUBLISH_ON_IMPORT_NONE = "none";

    /**
     * An option of {@link #publishOnImport} value, to publish a document automatically on import.
     */
    public static final String PUBLISH_ON_IMPORT_ALL = "all";

    /**
     * An option of {@link #publishOnImport} value, to publish a document automatically if the source of the content
     * has <code>hippo:availability</code> property including {@code live} value.
     */
    public static final String PUBLISH_ON_IMPORT_LIVE = "live";

    /**
     * The default option of {@link #publishOnImport} value.
     */
    public static final String PUBLISH_ON_IMPORT_DEFAULT = PUBLISH_ON_IMPORT_NONE;

    /**
     * Default gallery folder's primary node type name.
     */
    private static final String DEFAULT_GALLERY_FOLDER_PRIMARY_TYPE = "hippogallery:stdImageGallery";

    /**
     * Default foldertypes property values of a gallery folder.
     */
    private static final String[] DEFAULT_GALLERY_FOLDER_FOLDER_TYPES = { "new-image-folder" };

    /**
     * Default gallerytypes property values of a gallery folder.
     */
    private static final String[] DEFAULT_GALLERY_FOLDER_GALLERY_TYPES = { "hippogallery:imageset" };

    /**
     * Default asset folder's primary node type name.
     */
    private static final String DEFAULT_ASSET_FOLDER_PRIMARY_TYPE = "hippogallery:stdAssetGallery";

    /**
     * Default foldertypes property values of an asset folder.
     */
    private static final String[] DEFAULT_ASSET_FOLDER_FOLDER_TYPES = { "new-file-folder" };

    /**
     * Default gallerytypes property values of an asset folder.
     */
    private static final String[] DEFAULT_ASSET_FOLDER_GALLERY_TYPES = { "hippogallery:exampleAssetSet" };

    private Integer batchSize;
    private Long throttle;
    private String publishOnImport = PUBLISH_ON_IMPORT_DEFAULT;
    private Long dataUrlSizeThreshold;
    private QueriesAndPaths binaries;
    private QueriesAndPaths documents;
    private Set<String> docbasePropNames;
    private Set<String> documentTags;
    private Set<String> binaryTags;

    private String galleryFolderPrimaryType;
    private String[] galleryFolderFolderTypes;
    private String[] galleryFolderGalleryTypes;

    private String assetFolderPrimaryType;
    private String[] assetFolderFolderTypes;
    private String[] assetFolderGalleryTypes;

    public Integer getBatchSize() {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Long getThrottle() {
        if (throttle == null) {
            return DEFAULT_THROTTLE;
        }
        return throttle;
    }

    public void setThrottle(Long throttle) {
        this.throttle = throttle;
    }

    public String getPublishOnImport() {
        return publishOnImport;
    }

    public void setPublishOnImport(String publishOnImport) {
        if (StringUtils.equalsIgnoreCase(publishOnImport, PUBLISH_ON_IMPORT_LIVE)) {
            this.publishOnImport = PUBLISH_ON_IMPORT_LIVE;
        } else if (StringUtils.equalsIgnoreCase(publishOnImport, PUBLISH_ON_IMPORT_ALL) || BooleanUtils.toBoolean(publishOnImport)) {
            this.publishOnImport = PUBLISH_ON_IMPORT_ALL;
        } else {
            this.publishOnImport = PUBLISH_ON_IMPORT_NONE;
        }
    }

    public Long getDataUrlSizeThreshold() {
        if (dataUrlSizeThreshold == null) {
            return DEFAULT_DATA_URL_SIZE_THRESHOLD;
        }
        return dataUrlSizeThreshold;
    }

    public void setDataUrlSizeThreshold(Long dataUrlSizeThreshold) {
        this.dataUrlSizeThreshold = dataUrlSizeThreshold;
    }

    public QueriesAndPaths getBinaries() {
        return binaries;
    }

    public void setBinaries(QueriesAndPaths binaries) {
        this.binaries = binaries;
    }

    public QueriesAndPaths getDocuments() {
        return documents;
    }

    public Set<String> getDocbasePropNames() {
        return docbasePropNames;
    }

    public void setDocbasePropNames(Set<String> docbasePropNames) {
        this.docbasePropNames = docbasePropNames;
    }

    public void setDocuments(QueriesAndPaths documents) {
        this.documents = documents;
    }

    public Set<String> getDocumentTags() {
        return documentTags;
    }

    public void setDocumentTags(Set<String> documentTags) {
        this.documentTags = documentTags;
    }

    public Set<String> getBinaryTags() {
        return binaryTags;
    }

    public void setBinaryTags(Set<String> binaryTags) {
        this.binaryTags = binaryTags;
    }

    public String getGalleryFolderPrimaryType() {
        if (StringUtils.isBlank(galleryFolderPrimaryType)) {
            return DEFAULT_GALLERY_FOLDER_PRIMARY_TYPE;
        }
        return galleryFolderPrimaryType;
    }

    public void setGalleryFolderPrimaryType(String galleryFolderPrimaryType) {
        this.galleryFolderPrimaryType = galleryFolderPrimaryType;
    }

    public String[] getGalleryFolderFolderTypes() {
        if (ArrayUtils.isEmpty(galleryFolderFolderTypes)) {
            return DEFAULT_GALLERY_FOLDER_FOLDER_TYPES;
        }
        return galleryFolderFolderTypes;
    }

    public void setGalleryFolderFolderTypes(String[] galleryFolderFolderTypes) {
        this.galleryFolderFolderTypes = galleryFolderFolderTypes;
    }

    public String[] getGalleryFolderGalleryTypes() {
        if (ArrayUtils.isEmpty(galleryFolderGalleryTypes)) {
            return DEFAULT_GALLERY_FOLDER_GALLERY_TYPES;
        }
        return galleryFolderGalleryTypes;
    }

    public void setGalleryFolderGalleryTypes(String[] galleryFolderGalleryTypes) {
        this.galleryFolderGalleryTypes = galleryFolderGalleryTypes;
    }

    public String getAssetFolderPrimaryType() {
        if (StringUtils.isBlank(assetFolderPrimaryType)) {
            return DEFAULT_ASSET_FOLDER_PRIMARY_TYPE;
        }
        return assetFolderPrimaryType;
    }

    public void setAssetFolderPrimaryType(String assetFolderPrimaryType) {
        this.assetFolderPrimaryType = assetFolderPrimaryType;
    }

    public String[] getAssetFolderFolderTypes() {
        if (ArrayUtils.isEmpty(assetFolderFolderTypes)) {
            return DEFAULT_ASSET_FOLDER_FOLDER_TYPES;
        }
        return assetFolderFolderTypes;
    }

    public void setAssetFolderFolderTypes(String[] assetFolderFolderTypes) {
        this.assetFolderFolderTypes = assetFolderFolderTypes;
    }

    public String[] getAssetFolderGalleryTypes() {
        if (ArrayUtils.isEmpty(assetFolderGalleryTypes)) {
            return DEFAULT_ASSET_FOLDER_GALLERY_TYPES;
        }
        return assetFolderGalleryTypes;
    }

    public void setAssetFolderGalleryTypes(String[] assetFolderGalleryTypes) {
        this.assetFolderGalleryTypes = assetFolderGalleryTypes;
    }

}
