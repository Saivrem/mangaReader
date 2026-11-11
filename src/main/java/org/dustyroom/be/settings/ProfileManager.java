package org.dustyroom.be.settings;

import org.dustyroom.be.utils.SqliteUtils;
import org.dustyroom.be.persistence.schema.SqlitePragmas;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.persistence.schema.ProfileSql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;

/**
 * Explicit profile switching only. No backups, merging, or release-dependent filenames.
 */
@UtilityClass
public class ProfileManager {
    public static Path resolve(Path standardDirectory, String override) {
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize().resolve("reader.sqlite3");
        }
        try {
            ProfileSwitchJournal.recover(standardDirectory);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot finish the pending profile switch; existing data was kept.", exception);
        }
        Path pointer = standardDirectory.resolve("profile.path");
        if (!Files.exists(pointer)) {
            return standardDirectory.resolve("reader.sqlite3");
        }
        try {
            Path selected = Path.of(Files.readString(pointer, StandardCharsets.UTF_8).strip());
            if (!selected.isAbsolute()) {
                throw new IOException("Profile path must be absolute");
            }
            Path database = selected.resolve("reader.sqlite3");
            if (!Files.isRegularFile(database)) {
                throw new IOException("Selected reader profile is unavailable: " + selected);
            }
            return database.normalize();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot resolve the selected reader profile; no new database was created.", exception);
        }
    }

    public static boolean containsProfile(Path directory) {
        return Files.exists(directory.resolve("reader.sqlite3"));
    }

    public static Path validateTarget(Path currentDatabase, Path target, boolean existing) throws IOException {
        Path directory = target.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        if (Files.isSameFile(currentDatabase.getParent(), directory)) {
            throw new IOException("This is already the active data directory.");
        }
        if (!Files.isWritable(directory)) {
            throw new IOException("The selected folder is not writable.");
        }
        Path database = directory.resolve("reader.sqlite3");
        if (existing != Files.exists(database)) {
            throw new IOException("The target profile changed. Select the folder again.");
        }
        if (!existing && (Files.exists(directory.resolve("reader.sqlite3-wal")) || Files.exists(directory.resolve("reader.sqlite3-shm")))) {
            throw new IOException("The target contains an unfinished SQLite profile.");
        }

        return directory;
    }

    /**
     * Caller must close its ReaderStore and drain pending saves first.
     */
    public static void switchProfile(Path standardDirectory, Path currentDatabase, Path target, boolean existing) throws IOException {
        Path directory = validateTarget(currentDatabase, target, existing);
        Path destination = directory.resolve("reader.sqlite3");
        boolean moved = false;
        boolean committed = false;
        try {
            if (!existing) {
                finishJournal(currentDatabase);
                ProfileSwitchJournal.begin(standardDirectory, currentDatabase, destination);
                Files.move(currentDatabase, destination);
                moved = true;
            } else {
                validateSqlite(destination);
            }
            writePointer(standardDirectory, directory);
            committed = true;
            Files.deleteIfExists(ProfileSwitchJournal.file(standardDirectory));
        } catch (IOException exception) {
            if (committed) {
                // The selector already points at the intact destination. Startup cleans the metadata.
                return;
            }
            if (moved) {
                try {
                    Files.move(destination, currentDatabase);
                    Files.deleteIfExists(ProfileSwitchJournal.file(standardDirectory));
                } catch (IOException rollback) {
                    exception.addSuppressed(rollback);
                }
            }
            throw exception;
        }
    }

    private static void validateSqlite(Path database) throws IOException {
        try (Connection connection = SqliteUtils.connect(database);
             Statement sql = connection.createStatement(); ResultSet result = sql.executeQuery(ProfileSql.CHECK_DATABASE)) {
            if (!result.next() || !"ok".equals(result.getString(1))) {
                throw new IOException("The selected profile is not a readable SQLite database.");
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot open the selected profile.", exception);
        }
    }

    private static void finishJournal(Path database) throws IOException {
        try (Connection connection = SqliteUtils.connect(database);
             Statement sql = connection.createStatement()) {
            sql.execute(SqlitePragmas.BUSY_TIMEOUT);
            try (ResultSet rows = sql.executeQuery(ProfileSql.CHECKPOINT)) {
                if (rows.next() && rows.getInt(1) != 0) {
                    throw new IOException("The profile is in use by another reader.");
                }
            }
            try (ResultSet rows = sql.executeQuery(ProfileSql.CLOSE_JOURNAL)) {
                if (!rows.next() || !"delete".equalsIgnoreCase(rows.getString(1))) {
                    throw new IOException("The profile is still in use.");
                }
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot close the profile journal. Close other reader instances.", exception);
        }
    }

    static void writePointer(Path standardDirectory, Path directory) throws IOException {
        Files.createDirectories(standardDirectory);
        Path temporary = Files.createTempFile(standardDirectory, "profile-", ".tmp");
        try {
            Files.writeString(temporary, directory.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, standardDirectory.resolve("profile.path"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("The profile selector cannot be updated atomically on this filesystem.", exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
