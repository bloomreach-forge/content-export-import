/*
 * Copyright 2025 Bloomreach B.V. (https://www.bloomreach.com)
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
package org.onehippo.forge.content.exim.repository.jaxrs;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the package-private applyManifestOrdering in ContentEximImportService.
 *
 * Root cause of the ordering bug:
 *   baseFolder.getName().getPath() returned the ZIP's on-disk path
 *   (e.g. /tmp/foo.zip), not the ZIP-internal root "/".  Rank keys got the
 *   disk path prepended; f.getName().getPath() returned only the ZIP-internal
 *   path — zero matches — stable sort was a no-op — VFS HashMap order prevailed.
 *
 * Fix: use getRelativeName() which strips the base-folder prefix correctly
 *   regardless of how the ZIP was opened (zip:/path vs zip:file:///path).
 */
class ApplyManifestOrderingTest {

    @TempDir
    Path tempDir;

    private final ContentEximImportService service = new ContentEximImportService();

    /**
     * Manifest lists paths in reverse-alphabetical order (z → m → a).
     * applyManifestOrdering must return FileObject[] in manifest order, not VFS
     * hash order and not alphabetical order.
     */
    @Test
    void sortsFilesByManifestOrderNotVfsHashOrder() throws Exception {
        List<String> manifestOrder = List.of(
                "content/documents/knowledgebase/docs/getting-started/z-page/z-page.json",
                "content/documents/knowledgebase/docs/getting-started/m-page/m-page.json",
                "content/documents/knowledgebase/docs/getting-started/a-page/a-page.json"
        );

        Path zipFile = buildZipWithManifest(tempDir.resolve("ordered.zip"), manifestOrder);

        FileSystemManager vfs = VFS.getManager();
        FileObject baseFolder = vfs.resolveFile("zip:" + zipFile.toUri());
        FileObject[] jsonFiles = jsonFiles(baseFolder);

        FileObject[] sorted = service.applyManifestOrdering(baseFolder, jsonFiles);

        List<String> sortedNames = Arrays.stream(sorted)
                .map(f -> f.getName().getBaseName())
                .toList();

        // Manifest order: z-page, m-page, a-page — NOT alphabetical (a, m, z)
        assertThat(sortedNames).containsExactly("z-page.json", "m-page.json", "a-page.json");
    }

    /** Without a manifest the input array is returned unchanged (no crash, no reorder). */
    @Test
    void returnsUnchangedWhenNoManifestPresent() throws Exception {
        List<String> entries = List.of(
                "content/documents/docs/a/a.json",
                "content/documents/docs/b/b.json"
        );
        Path zipFile = buildZipNoManifest(tempDir.resolve("no-manifest.zip"), entries);

        FileSystemManager vfs = VFS.getManager();
        FileObject baseFolder = vfs.resolveFile("zip:" + zipFile.toUri());
        FileObject[] files = jsonFiles(baseFolder);

        FileObject[] result = service.applyManifestOrdering(baseFolder, files);
        assertThat(result).hasSize(2);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private FileObject[] jsonFiles(FileObject baseFolder) throws Exception {
        FileObject[] all = baseFolder.findFiles(new AllFileSelector());
        return Arrays.stream(all)
                .filter(f -> {
                    try { return f.getType() == FileType.FILE
                                 && f.getName().getBaseName().endsWith(".json"); }
                    catch (Exception e) { return false; }
                })
                .toArray(FileObject[]::new);
    }

    private Path buildZipWithManifest(Path zip, List<String> jsonPaths) throws Exception {
        try (OutputStream fos = Files.newOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            StringBuilder manifest = new StringBuilder("# EXIM manifest\n");
            for (String p : jsonPaths) manifest.append(p).append("\n");
            writeEntry(zos, "EXIM-INF/manifest.txt",
                    manifest.toString().getBytes(StandardCharsets.UTF_8));
            for (String p : jsonPaths) writeEntry(zos, p, "{}".getBytes());
        }
        return zip;
    }

    private Path buildZipNoManifest(Path zip, List<String> jsonPaths) throws Exception {
        try (OutputStream fos = Files.newOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String p : jsonPaths) writeEntry(zos, p, "{}".getBytes());
        }
        return zip;
    }

    private void writeEntry(ZipOutputStream zos, String name, byte[] data) throws Exception {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }
}
