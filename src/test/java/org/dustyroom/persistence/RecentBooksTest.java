package org.dustyroom.persistence;

import org.dustyroom.ui.navigation.ReadingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentBooksTest {
    @TempDir
    Path directory;

    private BookState book(String id, int page) {
        return new BookState(id, "page-" + page, page, 10, true, ReadingMode.COMICS);
    }

    @Test
    void hidingSurvivesProgressUpdatesAndRestartButExplicitOpenRestoresIt() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        Path file = Files.writeString(directory.resolve("日本語 book.zip"), "untouched");
        BookState state = book(file.toString(), 3);
        try (var store = new SqliteReaderStore(db)) {
            store.saveBook(state);
            store.hideRecent(state.sourceId());
            store.saveBook(book(state.sourceId(), 4));
            assertTrue(store.recentBooks(12).isEmpty());
            assertEquals(4, store.book(state.sourceId()).orElseThrow().pageNumber());
        }
        try (var store = new SqliteReaderStore(db)) {
            assertTrue(store.recentBooks(12).isEmpty());
            store.showRecent(state.sourceId());
            assertEquals(4, store.recentBooks(12).get(0).pageNumber());
            store.reset();
            assertTrue(store.recentBooks(12).isEmpty());
        }
        assertEquals("untouched", Files.readString(file));
    }

    @Test
    void historyIsOrderedBoundedAndDoesNotDuplicateBooks() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (var store = new SqliteReaderStore(db)) {
            for (int i = 0; i < 15; i++) store.saveBook(book("book-" + i, 1));
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + db);
                 var sql = connection.createStatement()) {
                sql.execute("UPDATE books SET updated_at=CAST(substr(source_id,6) AS INTEGER)");
            }
            assertEquals(12, store.recentBooks(12).size());
            assertEquals("book-14", store.recentBooks(12).get(0).sourceId());
            store.saveBook(book("book-0", 5));
            assertEquals("book-0", store.recentBooks(12).get(0).sourceId());
            assertEquals(15, store.recentBooks(100).size());
        }
    }

    @Test
    void upgradesSchemaOneWithoutLosingStateAndMakesReadableBackup() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + db);
             var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            sql.execute("INSERT INTO settings VALUES('theme','LIGHT')");
            sql.execute("CREATE TABLE books(source_id TEXT PRIMARY KEY,page_id TEXT NOT NULL,page_number INTEGER NOT NULL,page_count INTEGER NOT NULL,two_page INTEGER NOT NULL,reading_mode TEXT NOT NULL,updated_at INTEGER NOT NULL)");
            sql.execute("INSERT INTO books VALUES('saved.zip','page-3',3,10,1,'COMICS',123)");
            sql.execute("PRAGMA user_version=1");
        }
        try (var store = new SqliteReaderStore(db)) {
            assertEquals(book("saved.zip", 3), store.recentBooks(12).get(0));
            assertEquals(org.dustyroom.ui.theme.ViewerTheme.LIGHT, store.settings().theme());
        }
        Path backup;
        try (var files = Files.list(directory)) {
            backup = files.filter(p -> p.getFileName().toString().endsWith(".bak")).findFirst().orElseThrow();
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + backup);
             var sql = connection.createStatement()) {
            try (var result = sql.executeQuery("PRAGMA user_version")) {
                assertEquals(1, result.getInt(1));
            }
            try (var result = sql.executeQuery("SELECT page_number FROM books")) {
                assertEquals(3, result.getInt(1));
            }
        }
    }
}
