package org.dustyroom.persistence;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ViewerTheme;
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

import static org.junit.jupiter.api.Assertions.*;

class SqliteReaderStoreTest {
    @TempDir
    Path directory;

    @Test
    void persistsSettingsAndIndependentBooksAcrossReopen() {
        Path db = directory.resolve("nested/reader.sqlite3");
        ReaderSettings settings = new ReaderSettings(ViewerTheme.LIGHT, FitMode.ORIGINAL, 1.75, directory.resolve("日本語 books").toString(), false, false);
        BookState a = new BookState("/books/日本語 'vol'.zip", "chapter/02.png", 2, 10, true, ReadingMode.MANGA);
        BookState b = new BookState("/books/other", "/books/other/3.png", 3, 8, false, ReadingMode.COMICS);
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(a.getSourceId()).isEmpty());
            store.saveSettings(settings);
            store.saveBook(a);
            store.saveBook(b);
        }
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(settings, store.settings());
            assertEquals(a, store.book(a.getSourceId()).orElseThrow());
            assertEquals(b, store.book(b.getSourceId()).orElseThrow());
            store.saveBook(new BookState(a.getSourceId(), "chapter/09.png", 9, 10, false, ReadingMode.COMICS));
        }
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(9, store.book(a.getSourceId()).orElseThrow().getPageNumber());
            assertEquals(b, store.book(b.getSourceId()).orElseThrow());
        }
    }

    @Test
    void existingSettingsWithoutLastDirectoryRemainCompatible() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (ReaderStore store = new SqliteReaderStore(db)) {
            store.saveSettings(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1));
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
            sql.execute("DELETE FROM settings WHERE key='last_directory'");
        }
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1), store.settings());
        }
    }

    @Test
    void invalidConfigValuesFallBackWithoutLosingValidSettings() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (ReaderStore store = new SqliteReaderStore(db)) {
            store.saveSettings(ReaderSettings.defaults());
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
            sql.execute("UPDATE settings SET value='LIGHT' WHERE key='theme'");
            sql.execute("DELETE FROM settings WHERE key='theme_mode'");
            sql.execute("UPDATE settings SET value='not-a-mode' WHERE key='fit_mode'");
            sql.execute("UPDATE settings SET value='NaN' WHERE key='zoom'");
        }
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_SCREEN, 1), store.settings());
        }
    }

    @Test
    void resetClearsStoredStateButLeavesBookFilesAndSchemaIntact() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        Path book = directory.resolve("book.zip");
        byte[] original = {1, 2, 3};
        Files.write(book, original);
        try (ReaderStore store = new SqliteReaderStore(db)) {
            store.saveSettings(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1, directory.toString()));
            store.saveBook(new BookState(book.toString(), "page.png", 2, 5, true, ReadingMode.COMICS));
            store.reset();
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(book.toString()).isEmpty());
        }
        try (ReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(book.toString()).isEmpty());
        }
        assertArrayEquals(original, Files.readAllBytes(book));
    }

    @Test
    void failedResetRollsBackSettingsAndBooksTogether() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        ReaderSettings settings = new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1);
        BookState book = new BookState("book", "page", 2, 5, false, ReadingMode.MANGA);
        try (ReaderStore store = new SqliteReaderStore(db)) {
            store.saveSettings(settings);
            store.saveBook(book);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
                sql.execute("CREATE TRIGGER prevent_delete BEFORE DELETE ON books BEGIN SELECT RAISE(ABORT,'test failure'); END");
            }
            assertThrows(IllegalStateException.class, store::reset);
            assertEquals(settings, store.settings());
            assertEquals(book, store.book("book").orElseThrow());
        }
    }

    @Test
    void failedMigrationRollsBackDdlAndKeepsThePreviousSchemaVersion() throws Exception {
        Path db = directory.resolve("old.sqlite3");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE books (original TEXT)");
            sql.execute("INSERT INTO books VALUES('preserved')");
        }
        assertThrows(IllegalStateException.class, () -> new SqliteReaderStore(db));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
            try (ResultSet result = sql.executeQuery("PRAGMA user_version")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (ResultSet result = sql.executeQuery("SELECT original FROM books")) {
                assertTrue(result.next());
                assertEquals("preserved", result.getString(1));
            }
            try (ResultSet result = sql.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='settings'")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        }
    }

    @Test
    void refusesFutureSchemaWithoutDowngradingIt() throws Exception {
        Path db = directory.resolve("future.sqlite3");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement()) {
            sql.execute("PRAGMA user_version=4");
        }
        byte[] before = Files.readAllBytes(db);
        assertThrows(IllegalStateException.class, () -> new SqliteReaderStore(db));
        assertArrayEquals(before, Files.readAllBytes(db));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db); Statement sql = connection.createStatement();
             ResultSet result = sql.executeQuery("PRAGMA user_version")) {
            assertTrue(result.next());
            assertEquals(4, result.getInt(1));
        }
    }
}
