package org.dustyroom.library;

import org.dustyroom.be.library.LibraryScanner;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibraryScan;
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
    @TempDir
    Path directory;

    private static void archive(Path file, String page) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(page));
            zip.write(new byte[]{1});
            zip.closeEntry();
        }
    }

    @Test
    void groupsChildFoldersAndLooseImagesButKeepsRootArchivesSeparate() throws Exception {
        archive(directory.resolve("10.zip"), "p.png");
        archive(directory.resolve("2.zip"), "p.jpg");
        archive(directory.resolve("empty.zip"), "readme.txt");
        Files.writeString(directory.resolve("broken.zip"), "broken");
        Files.writeString(directory.resolve("cover.png"), "image decoding is deliberately deferred");
        Path nested = Files.createDirectory(directory.resolve("nested"));
        archive(nested.resolve("1.zip"), "p.gif");
        archive(nested.resolve("10.zip"), "page.png");
        Files.writeString(nested.resolve("cover.jpg"), "image");
        Files.createDirectory(nested.resolve("deeper"));
        archive(nested.resolve("deeper/ignored.zip"), "page.png");
        LibraryScan found = new LibraryScanner().scan(directory, () -> false);
        assertEquals(4, found.works().size());
        List<LibraryImport> singles = found.works()
                .stream()
                .filter(work -> work.getRoot().endsWith(".zip"))
                .toList();
        assertEquals(List.of("2.zip", "10.zip"), singles
                .stream()
                .map(LibraryImport::getTitle)
                .toList());
        for (LibraryImport work : singles) {
            assertEquals(work.getRoot(), work.getSources().getFirst().getPath());
            assertEquals(1, work.getSources().size());
        }
        LibraryImport loose = found.works()
                .stream()
                .filter(work -> directory.toString().equals(work.getRoot()))
                .findFirst()
                .orElseThrow();
        assertTrue(loose.getSources().getFirst().isDirectory());
        LibraryImport series = found.works()
                .stream()
                .filter(work -> nested.toString().equals(work.getRoot()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("nested", "1.zip", "10.zip"), series.getSources()
                .stream()
                .map(LibrarySource::getTitle)
                .toList());
        assertEquals(2, found.warnings().size());
    }

    @Test
    void cancellationDoesNotReturnPartialImport() throws Exception {
        archive(directory.resolve("1.zip"), "p.png");
        assertThrows(CancellationException.class, () -> new LibraryScanner().scan(directory, () -> true));
    }
}
