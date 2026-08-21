package org.dustyroom.be.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.dustyroom.be.utils.FileUtils.*;

class FileUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    void recognizesExactFinalExtensionsCaseInsensitively() {
        assertTrue(isSupportedImage("chapter/page01.JPG"));
        assertTrue(isSupportedImage("chapter\\page01.PnG"));
        assertTrue(isSupportedArchive("Volume.ZIP"));

        assertFalse(isSupportedImage("pagejpg"));
        assertFalse(isSupportedImage("page.jpg.backup"));
        assertFalse(isSupportedImage(".jpg"));
        assertFalse(isSupportedImage("page."));
        assertFalse(isSupportedImage("volume.zip"));
        assertFalse(isSupportedArchive("page.png"));
        assertFalse(isSupportedImage(null));
    }

    @Test
    void filtersMetadataPaths() {
        assertFalse(isSupportedImage("__MACOSX/chapter/page.jpg"));
        assertFalse(isSupportedImage("chapter/.DS_Store.jpg"));
    }

    @Test
    void filePredicatesRequireTheExpectedRealFileType() throws Exception {
        Path image = Files.createFile(tempDir.resolve("page.JPG"));
        Path archive = Files.createFile(tempDir.resolve("volume.ZIP"));
        Files.createDirectory(tempDir.resolve("folder.jpg"));

        assertTrue(isImage.test(image.toFile()));
        assertTrue(isZipFile.test(archive.toFile()));
        assertFalse(isImage.test(archive.toFile()));
        assertFalse(isZipFile.test(image.toFile()));
        assertFalse(isImage.test(tempDir.resolve("folder.jpg").toFile()));
    }

    @Test
    void returnsOnlyNaturallySortedMatchingFiles() throws Exception {
        Files.createFile(tempDir.resolve("page10.png"));
        Files.createFile(tempDir.resolve("page2.JPG"));
        Files.createFile(tempDir.resolve("page1.jpeg"));
        Files.createFile(tempDir.resolve("volume.zip"));
        Files.createFile(tempDir.resolve("notes.txt"));

        List<String> names = getSortedFiles(tempDir.toFile(), isImage).stream()
                .map(File::getName)
                .toList();

        assertEquals(List.of("page1.jpeg", "page2.JPG", "page10.png"), names);
        assertTrue(getSortedFilesFromParent(null, isImage).isEmpty());
    }

    @Test
    void distinguishesNonEmptyDirectoriesFromDirectoriesContainingImages() throws Exception {
        Path textOnly = Files.createDirectory(tempDir.resolve("text-only"));
        Files.createFile(textOnly.resolve("notes.txt"));

        assertTrue(isNotEmptyDirectory.test(textOnly.toFile()));
        assertFalse(containsImages.test(textOnly.toFile()));
    }
}
