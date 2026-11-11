package org.dustyroom.be.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.dustyroom.be.utils.FileUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {
    @TempDir
    Path directory;

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
        Path image = Files.createFile(directory.resolve("page.JPG"));
        Path archive = Files.createFile(directory.resolve("volume.ZIP"));
        Files.createDirectory(directory.resolve("folder.jpg"));

        assertTrue(isImage.test(image.toFile()));
        assertTrue(isZipFile.test(archive.toFile()));
        assertFalse(isImage.test(archive.toFile()));
        assertFalse(isZipFile.test(image.toFile()));
        assertFalse(isImage.test(directory.resolve("folder.jpg").toFile()));
    }

    @Test
    void returnsOnlyNaturallySortedMatchingFiles() throws Exception {
        Files.createFile(directory.resolve("page10.png"));
        Files.createFile(directory.resolve("page2.JPG"));
        Files.createFile(directory.resolve("page1.jpeg"));
        Files.createFile(directory.resolve("volume.zip"));
        Files.createFile(directory.resolve("notes.txt"));

        List<String> names = getSortedFiles(directory.toFile(), isImage)
                .stream()
                .map(File::getName)
                .toList();

        assertEquals(List.of("page1.jpeg", "page2.JPG", "page10.png"), names);
        assertTrue(getSortedFilesFromParent(null, isImage).isEmpty());
    }

    @Test
    void distinguishesNonEmptyDirectoriesFromDirectoriesContainingImages() throws Exception {
        Path textOnly = Files.createDirectory(directory.resolve("text-only"));
        Files.createFile(textOnly.resolve("notes.txt"));

        assertTrue(isNotEmptyDirectory.test(textOnly.toFile()));
        assertFalse(containsImages.test(textOnly.toFile()));
    }

    @Test
    void usesExistingSavedDirectory() throws Exception {
        Path saved = Files.createDirectory(directory.resolve("日本語 books"));
        assertEquals(saved.toFile(), FileUtils.resolve(saved.toString(), directory.toString()));
    }

    @Test
    void fallsBackForMissingFileRelativeAndMalformedPaths() throws Exception {
        Path file = Files.createFile(directory.resolve("book.zip"));
        Path deleted = Files.createDirectory(directory.resolve("deleted"));
        Files.delete(deleted);
        for (String saved : new String[]{null, "", " ", deleted.toString(), file.toString(), "relative", "invalid\0path"}) {
            assertEquals(directory.toFile(), FileUtils.resolve(saved, directory.toString()));
        }
    }
}
