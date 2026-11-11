package org.dustyroom.persistence;

import org.dustyroom.be.utils.SqliteUtils;
import org.dustyroom.be.persistence.schema.SqlitePragmas;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

abstract class BasePersistenceTest {
    @TempDir
    Path directory;
    Path database;

    @BeforeEach
    void selectDatabase() {
        database = directory.resolve("reader.sqlite3");
    }

    SqliteReaderStore openStore() {
        return new SqliteReaderStore(database);
    }

    void execute(PersistenceSql query, Object... parameters) throws SQLException {
        try (Connection connection = SqliteUtils.connect(database);
             PreparedStatement statement = SqliteUtils.prepare(connection, query.getSql(), parameters)) {
            statement.execute();
        }
    }

    Object scalar(PersistenceSql query, Object... parameters) throws SQLException {
        try (Connection connection = SqliteUtils.connect(database);
             PreparedStatement statement = SqliteUtils.prepare(connection, query.getSql(), parameters);
             ResultSet rows = statement.executeQuery()) {
            assertTrue(rows.next(), query.name());
            Object value = rows.getObject(1);
            assertFalse(rows.next(), query.name());
            return value;
        }
    }

    void setSchemaVersion(int version) throws SQLException {
        try (Connection connection = SqliteUtils.connect(database); Statement statement = connection.createStatement()) {
            statement.execute(SqlitePragmas.schemaVersion(version));
        }
    }

    void assertOpenRejectedWithoutChanges() throws IOException {
        byte[] before = Files.readAllBytes(database);
        assertThrows(IllegalStateException.class, this::openStore);
        assertArrayEquals(before, Files.readAllBytes(database));
    }

    void assertNoBackups() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".bak")));
        }
    }
}
