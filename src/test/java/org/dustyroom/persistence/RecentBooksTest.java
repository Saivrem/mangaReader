package org.dustyroom.persistence;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Stream;

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
        try (SqliteReaderStore store = new SqliteReaderStore(db)) {
            store.saveBook(state);
            store.hideRecent(state.getSourceId());
            store.saveBook(book(state.getSourceId(), 4));
            assertTrue(store.recentBooks(12).isEmpty());
            assertEquals(4, store.book(state.getSourceId()).orElseThrow().getPageNumber());
        }
        try (SqliteReaderStore store = new SqliteReaderStore(db)) {
            assertTrue(store.recentBooks(12).isEmpty());
            store.showRecent(state.getSourceId());
            assertEquals(4, store.recentBooks(12).get(0).getPageNumber());
            store.reset();
            assertTrue(store.recentBooks(12).isEmpty());
        }
        assertEquals("untouched", Files.readString(file));
    }

    @Test
    void historyIsOrderedBoundedAndDoesNotDuplicateBooks() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (SqliteReaderStore store = new SqliteReaderStore(db)) {
            for (int i = 0; i < 15; i++) store.saveBook(book("book-" + i, 1));
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db);
                 Statement sql = connection.createStatement()) {
                sql.execute("UPDATE books SET updated_at=CAST(substr(source_id,6) AS INTEGER)");
            }
            assertEquals(12, store.recentBooks(12).size());
            assertEquals("book-14", store.recentBooks(12).get(0).getSourceId());
            store.saveBook(book("book-0", 5));
            assertEquals("book-0", store.recentBooks(12).get(0).getSourceId());
            assertEquals(15, store.recentBooks(100).size());
        }
    }

    @Test
    void upgradesSchemaOneWithoutLosingStateOrCreatingCopies() throws Exception {
        Path db = directory.resolve("reader.sqlite3");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db);
             Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            sql.execute("INSERT INTO settings VALUES('theme','LIGHT')");
            sql.execute("CREATE TABLE books(source_id TEXT PRIMARY KEY,page_id TEXT NOT NULL,page_number INTEGER NOT NULL,page_count INTEGER NOT NULL,two_page INTEGER NOT NULL,reading_mode TEXT NOT NULL,updated_at INTEGER NOT NULL)");
            sql.execute("INSERT INTO books VALUES('saved.zip','page-3',3,10,1,'COMICS',123)");
            sql.execute("PRAGMA user_version=1");
        }
        try (SqliteReaderStore store = new SqliteReaderStore(db)) {
            assertEquals(book("saved.zip", 3), store.recentBooks(12).get(0));
            assertEquals(ViewerTheme.LIGHT, store.settings().getTheme());
        }
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".bak")));
        }
    }
}
