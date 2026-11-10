package org.dustyroom.persistence;

import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.persistence.RecoverableReaderStore;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseRecoveryTest {
    @TempDir Path directory;

    @Test void explicitResetOfFutureSchemaAllowsFreshStartupWithoutCreatingCopies() throws Exception {
        Path database = directory.resolve("reader.sqlite3");
        Path book = directory.resolve("book.zip");
        Files.writeString(book, "never owned by the reader");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database); Statement sql = connection.createStatement()) {
            sql.execute("PRAGMA user_version=99");
            sql.execute("CREATE TABLE future_data(value TEXT)");
            sql.execute("INSERT INTO future_data VALUES('future data')");
        }
        assertThrows(IllegalStateException.class, () -> new SqliteReaderStore(database));
        byte[] original = Files.readAllBytes(database);
        try (ReaderStore recovery = new RecoverableReaderStore(database)) {
            recovery.settings();
            assertArrayEquals(original, Files.readAllBytes(database), "Fallback startup must not reset anything");
            recovery.reset();
        }
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".bak")));
        }
        try (ReaderStore store = new SqliteReaderStore(database)) {
            assertTrue(store.libraryWorks().isEmpty());
            assertEquals(ReaderSettings.defaults(), store.settings());
        }
        assertEquals("never owned by the reader", Files.readString(book));
    }

    @Test void failedResetLeavesUnreadableDatabaseUntouched() throws Exception {
        Path database = directory.resolve("broken.sqlite3");
        Files.writeString(database, "not a sqlite database");
        byte[] original = Files.readAllBytes(database);
        try (ReaderStore recovery = new RecoverableReaderStore(database)) {
            assertThrows(IllegalStateException.class, recovery::reset);
        }
        assertArrayEquals(original, Files.readAllBytes(database));
    }
}
