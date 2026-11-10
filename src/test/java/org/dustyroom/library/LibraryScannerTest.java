package org.dustyroom.library;

import org.dustyroom.be.library.LibraryScanner;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LibraryScannerTest {
    @TempDir Path directory;

    @Test void scansOnlyImmediateSupportedSourcesInNaturalOrderAndReportsBadArchives() throws Exception {
        archive(directory.resolve("10.zip"), "p.png");
        archive(directory.resolve("2.zip"), "p.jpg");
        archive(directory.resolve("empty.zip"), "readme.txt");
        Files.writeString(directory.resolve("broken.zip"), "broken");
        Files.writeString(directory.resolve("cover.png"), "image decoding is deliberately deferred");
        Path nested = Files.createDirectory(directory.resolve("nested"));
        archive(nested.resolve("1.zip"), "p.gif");
        LibraryImport found = new LibraryScanner().scan(directory, () -> false);
        assertEquals(List.of(directory.getFileName().toString(), "2.zip", "10.zip"),
                found.getSources().stream().map(LibrarySource::getTitle).toList());
        assertTrue(found.getSources().getFirst().isDirectory());
        assertEquals(2, found.getWarnings().size());
    }

    @Test void cancellationDoesNotReturnPartialImport() throws Exception {
        archive(directory.resolve("1.zip"), "p.png");
        assertThrows(CancellationException.class, () -> new LibraryScanner().scan(directory, () -> true));
    }

    private static void archive(Path file, String page) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(page));
            zip.write(new byte[]{1});
            zip.closeEntry();
        }
    }
}
