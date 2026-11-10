package org.dustyroom.persistence;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LibraryStoreTest {
    @TempDir Path directory;

    private LibraryImport selection() {
        return new LibraryImport(directory.toString(), "夜中", List.of(
                new LibrarySource(directory.resolve("10.zip").toString(), "10.zip", false),
                new LibrarySource(directory.resolve("2.zip").toString(), "2.zip", false)), List.of());
    }

    @Test void libraryAndHistoryAreIndependentAndReimportPreservesIdentityAndCompletion() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        LibraryImport selection = selection();
        String source = selection.getSources().getFirst().getPath();
        Files.writeString(Path.of(source), "user book must survive");
        String workId;
        String chapterId;
        BookState saved = new BookState(source, "日本語.png", 4, 20, true, ReadingMode.COMICS);
        try (ReaderStore store = new SqliteReaderStore(database)) {
            store.saveBook(saved);
            store.hideRecent(source);
            store.importLibrary(selection);
            LibraryWork work = store.libraryWorks().getFirst();
            workId = work.getId();
            chapterId = work.getChapters().getLast().getId();
            assertEquals(List.of("2.zip", "10.zip"), work.getChapters().stream().map(chapter -> chapter.getSource().getTitle()).toList());
            assertEquals(4, work.getResumeChapter().getPage());
            assertTrue(store.recentBooks(12).isEmpty());
            store.finishChapter(chapterId, true);
            store.renameLibrary(workId, "Custom title");
            store.removeLibrary(workId);
            assertTrue(store.libraryWorks().isEmpty());
            assertEquals(saved, store.book(source).orElseThrow());
            store.importLibrary(selection);
            store.importLibrary(selection);
            work = store.libraryWorks().getFirst();
            assertEquals(workId, work.getId());
            assertEquals("Custom title", work.getTitle());
            assertEquals(2, work.getChapters().size());
            assertEquals(chapterId, work.getChapters().getLast().getId());
            assertEquals(1, work.getCompleted());
            assertTrue(store.recentBooks(12).isEmpty());
        }
        try (ReaderStore store = new SqliteReaderStore(database)) {
            assertEquals(workId, store.libraryWorks().getFirst().getId());
            store.reset();
            assertTrue(store.libraryWorks().isEmpty());
            assertTrue(store.book(source).isEmpty());
        }
        assertEquals("user book must survive", Files.readString(Path.of(source)));
    }

    @Test void migrationFromSchemaTwoKeepsHiddenStateWithoutCreatingCopies() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        BookState saved = new BookState("old-book", "page", 3, 7, true, ReadingMode.COMICS);
        try (ReaderStore store = new SqliteReaderStore(database)) {
            store.saveBook(saved);
            store.hideRecent(saved.getSourceId());
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
            sql.execute("DROP TABLE reader_components");
            sql.execute("DROP TABLE library_chapters");
            sql.execute("DROP TABLE library_works");
            sql.execute("PRAGMA user_version=2");
        }
        try (ReaderStore store = new SqliteReaderStore(database)) {
            assertEquals(saved, store.book(saved.getSourceId()).orElseThrow());
            assertTrue(store.recentBooks(12).isEmpty());
            assertTrue(store.libraryWorks().isEmpty());
        }
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().endsWith(".bak")).count());
        }
    }

    @Test void transitionalSchemaThreeConvertsWithoutLosingLibraryOrProgress() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        String id;
        BookState saved = new BookState(selection().getSources().getFirst().getPath(), "page", 3, 9, true, ReadingMode.COMICS);
        try (ReaderStore store = new SqliteReaderStore(database)) {
            store.importLibrary(selection());
            store.saveBook(saved);
            store.hideRecent(saved.getSourceId());
            id = store.libraryWorks().getFirst().getId();
            store.finishChapter(store.libraryWorks().getFirst().getChapters().getFirst().getId(), true);
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
            sql.execute("DROP TABLE reader_components");
            sql.execute("PRAGMA user_version=3");
        }
        try (ReaderStore store = new SqliteReaderStore(database)) {
            assertEquals(id, store.libraryWorks().getFirst().getId());
            assertEquals(1, store.libraryWorks().getFirst().getCompleted());
            assertEquals(saved, store.book(saved.getSourceId()).orElseThrow());
            assertTrue(store.recentBooks(12).isEmpty());
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement();
             ResultSet rows = sql.executeQuery("PRAGMA user_version")) {
            assertEquals(2, rows.getInt(1));
        }
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().endsWith(".bak")).count());
        }
    }

    @Test void unrecognizedSchemaThreeIsNeverDowngraded() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE future_data(value TEXT)");
            sql.execute("PRAGMA user_version=3");
        }
        byte[] before = Files.readAllBytes(database);
        assertThrows(IllegalStateException.class, () -> new SqliteReaderStore(database));
        assertArrayEquals(before, Files.readAllBytes(database));
    }

    @Test void rejectsFutureLibraryVersionWithoutChangingReaderData() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        try (ReaderStore store = new SqliteReaderStore(database)) { store.importLibrary(selection()); }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
            sql.execute("UPDATE reader_components SET version=2 WHERE component='library'");
        }
        byte[] before = Files.readAllBytes(database);
        assertThrows(IllegalStateException.class, () -> new SqliteReaderStore(database));
        assertArrayEquals(before, Files.readAllBytes(database));
    }

    @Test void failedImportRollsBackTheWholeWork() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        try (ReaderStore store = new SqliteReaderStore(database)) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
                sql.execute("CREATE TRIGGER prevent_chapter BEFORE INSERT ON library_chapters BEGIN SELECT RAISE(ABORT,'test failure'); END");
            }
            assertThrows(IllegalStateException.class, () -> store.importLibrary(selection()));
            assertTrue(store.libraryWorks().isEmpty());
        }
    }
}
