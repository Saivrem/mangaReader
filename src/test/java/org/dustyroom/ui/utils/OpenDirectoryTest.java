package org.dustyroom.ui.utils;

import org.dustyroom.be.utils.OpenDirectory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenDirectoryTest {
    @TempDir
    Path directory;

    @Test
    void usesExistingSavedDirectory() throws Exception {
        Path saved = Files.createDirectory(directory.resolve("日本語 books"));
        assertEquals(saved.toFile(), OpenDirectory.resolve(saved.toString(), directory.toString()));
    }

    @Test
    void fallsBackForMissingFileRelativeAndMalformedPaths() throws Exception {
        Path file = Files.createFile(directory.resolve("book.zip"));
        Path deleted = Files.createDirectory(directory.resolve("deleted"));
        Files.delete(deleted);
        for (String saved : new String[]{null, "", " ", deleted.toString(), file.toString(), "relative", "invalid\0path"}) {
            assertEquals(directory.toFile(), OpenDirectory.resolve(saved, directory.toString()));
        }
    }
}
