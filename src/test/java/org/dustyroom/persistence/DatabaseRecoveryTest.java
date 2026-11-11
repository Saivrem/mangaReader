package org.dustyroom.persistence;

import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.persistence.RecoverableReaderStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.dustyroom.persistence.PersistenceSql.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseRecoveryTest extends BasePersistenceTest {

    @Test
    void explicitResetOfFutureSchemaAllowsFreshStartupWithoutCreatingCopies() throws Exception {
        Path book = directory.resolve("book.zip");
        Files.writeString(book, "never owned by the reader");
        setSchemaVersion(99);
        execute(CREATE_FUTURE_DATA);
        execute(INSERT_FUTURE_DATA, "future data");
        assertThrows(IllegalStateException.class, this::openStore);
        byte[] original = Files.readAllBytes(database);
        try (ReaderStore recovery = new RecoverableReaderStore(database)) {
            recovery.settings();
            assertArrayEquals(original, Files.readAllBytes(database), "Fallback startup must not reset anything");
            recovery.reset();
        }
        assertNoBackups();
        try (ReaderStore store = openStore()) {
            assertTrue(store.libraryWorks().isEmpty());
            assertEquals(ReaderSettings.defaults(), store.settings());
        }
        assertEquals("never owned by the reader", Files.readString(book));
    }

    @Test
    void failedResetLeavesUnreadableDatabaseUntouched() throws Exception {
        database = directory.resolve("broken.sqlite3");
        Files.writeString(database, "not a sqlite database");
        byte[] original = Files.readAllBytes(database);
        try (ReaderStore recovery = new RecoverableReaderStore(database)) {
            assertThrows(IllegalStateException.class, recovery::reset);
        }
        assertArrayEquals(original, Files.readAllBytes(database));
    }
}
