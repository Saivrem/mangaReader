package org.dustyroom.persistence;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.persistence.ReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.dustyroom.persistence.PersistenceSql.*;
import static org.junit.jupiter.api.Assertions.*;

class SqliteReaderStoreTest extends BasePersistenceTest {

    @Test
    void persistsSettingsAndIndependentBooksAcrossReopen() {
        database = directory.resolve("nested/reader.sqlite3");
        ReaderSettings settings = new ReaderSettings(ViewerTheme.LIGHT, FitMode.ORIGINAL, 1.75, directory.resolve("日本語 books").toString(), false, false);
        BookState a = new BookState("/books/日本語 'vol'.zip", "chapter/02.png", 2, 10, true, ReadingMode.MANGA);
        BookState b = new BookState("/books/other", "/books/other/3.png", 3, 8, false, ReadingMode.COMICS);
        try (ReaderStore store = openStore()) {
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(a.getSourceId()).isEmpty());
            store.saveSettings(settings);
            store.saveBook(a);
            store.saveBook(b);
        }
        try (ReaderStore store = openStore()) {
            assertEquals(settings, store.settings());
            assertEquals(a, store.book(a.getSourceId()).orElseThrow());
            assertEquals(b, store.book(b.getSourceId()).orElseThrow());
            store.saveBook(new BookState(a.getSourceId(), "chapter/09.png", 9, 10, false, ReadingMode.COMICS));
        }
        try (ReaderStore store = openStore()) {
            assertEquals(9, store.book(a.getSourceId()).orElseThrow().getPageNumber());
            assertEquals(b, store.book(b.getSourceId()).orElseThrow());
        }
    }

    @Test
    void existingSettingsWithoutLastDirectoryRemainCompatible() throws Exception {
        try (ReaderStore store = openStore()) {
            store.saveSettings(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1));
        }
        execute(DELETE_SETTING, "last_directory");
        try (ReaderStore store = openStore()) {
            assertEquals(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1), store.settings());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "invalid", "0", "21"})
    void invalidConfigValuesFallBackWithoutLosingValidSettings(String invalidZoom) throws Exception {
        try (ReaderStore store = openStore()) {
            store.saveSettings(ReaderSettings.defaults());
        }
        execute(UPDATE_SETTING, "LIGHT", "theme");
        execute(DELETE_SETTING, "theme_mode");
        execute(UPDATE_SETTING, "not-a-mode", "fit_mode");
        execute(UPDATE_SETTING, invalidZoom, "zoom");
        try (ReaderStore store = openStore()) {
            assertEquals(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_SCREEN, 1), store.settings());
        }
    }

    @Test
    void resetClearsStoredStateButLeavesBookFilesAndSchemaIntact() throws Exception {
        Path book = directory.resolve("book.zip");
        byte[] original = {1, 2, 3};
        Files.write(book, original);
        try (ReaderStore store = openStore()) {
            store.saveSettings(new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1, directory.toString()));
            store.saveBook(new BookState(book.toString(), "page.png", 2, 5, true, ReadingMode.COMICS));
            store.reset();
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(book.toString()).isEmpty());
        }
        try (ReaderStore store = openStore()) {
            assertEquals(ReaderSettings.defaults(), store.settings());
            assertTrue(store.book(book.toString()).isEmpty());
        }
        assertArrayEquals(original, Files.readAllBytes(book));
    }

    @Test
    void failedResetRollsBackSettingsAndBooksTogether() throws Exception {
        ReaderSettings settings = new ReaderSettings(ViewerTheme.LIGHT, FitMode.FIT_WIDTH, 1);
        BookState book = new BookState("book", "page", 2, 5, false, ReadingMode.MANGA);
        try (ReaderStore store = openStore()) {
            store.saveSettings(settings);
            store.saveBook(book);
            execute(BLOCK_BOOK_DELETE);
            assertThrows(IllegalStateException.class, store::reset);
            assertEquals(settings, store.settings());
            assertEquals(book, store.book("book").orElseThrow());
        }
    }

    @Test
    void failedMigrationRollsBackDdlAndKeepsThePreviousSchemaVersion() throws Exception {
        database = directory.resolve("old.sqlite3");
        execute(CREATE_ORIGINAL_BOOKS);
        execute(INSERT_ORIGINAL_BOOK, "preserved");
        assertThrows(IllegalStateException.class, this::openStore);
        assertEquals(0, scalar(SCHEMA_VERSION));
        assertEquals("preserved", scalar(READ_ORIGINAL_BOOK));
        assertEquals(0, scalar(COUNT_TABLES, "settings"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 99})
    void refusesFutureSchemaWithoutDowngradingIt(int version) throws Exception {
        database = directory.resolve("future.sqlite3");
        setSchemaVersion(version);
        assertOpenRejectedWithoutChanges();
        assertEquals(version, scalar(SCHEMA_VERSION));
    }
}
