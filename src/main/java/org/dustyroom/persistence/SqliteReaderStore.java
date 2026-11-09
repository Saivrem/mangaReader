package org.dustyroom.persistence;

import org.dustyroom.ui.components.ImageComponent.FitMode;
import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.ui.theme.ViewerTheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SqliteReaderStore implements ReaderStore {
    // Independent of the application release version (0.8, 0.9, ...).
    static final int SCHEMA_VERSION = 1;
    private final Connection connection;

    public SqliteReaderStore(Path database) {
        Connection opened = null;
        try {
            Path path = database.toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            opened = DriverManager.getConnection("jdbc:sqlite:" + path);
            connection = opened;
            int version;
            try (var sql = connection.createStatement()) {
                sql.execute("PRAGMA busy_timeout=3000");
                try (var result = sql.executeQuery("PRAGMA user_version")) {
                    version = result.next() ? result.getInt(1) : 0;
                    if (version > SCHEMA_VERSION) throw new SQLException("Database schema is newer than this application; no changes were made");
                }
            }
            if (version > 0 && version < SCHEMA_VERSION) backupBeforeMigration(path, version);
            migrate(version);
            try (var sql = connection.createStatement()) { sql.execute("PRAGMA journal_mode=WAL"); }
        } catch (Exception exception) {
            if (opened != null) try { opened.close(); } catch (SQLException closeError) { exception.addSuppressed(closeError); }
            throw new IllegalStateException("Cannot open reader database: " + database, exception);
        }
    }

    Path backupBeforeMigration(Path database, int version) throws SQLException {
        Path backup = database.resolveSibling(database.getFileName() + ".schema-" + version + "."
                + java.util.UUID.randomUUID() + ".bak");
        // SQLite creates a consistent snapshot including committed WAL contents.
        // If backup fails, the upgrade must not start.
        try (var sql = connection.prepareStatement("VACUUM INTO ?")) {
            sql.setString(1, backup.toString());
            sql.execute();
        }
        return backup;
    }

    private void migrate(int version) throws SQLException {
        if (version == SCHEMA_VERSION) return;
        connection.setAutoCommit(false);
        try (var sql = connection.createStatement()) {
            // Add future migrations in version order; never recreate existing tables on startup.
            if (version < 1) {
                sql.execute("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
                sql.execute("""
                        CREATE TABLE books (
                            source_id TEXT PRIMARY KEY,
                            page_id TEXT NOT NULL,
                            page_number INTEGER NOT NULL CHECK(page_number >= 1),
                            page_count INTEGER NOT NULL CHECK(page_count >= page_number),
                            two_page INTEGER NOT NULL CHECK(two_page IN (0,1)),
                            reading_mode TEXT NOT NULL CHECK(reading_mode IN ('MANGA','COMICS')),
                            updated_at INTEGER NOT NULL
                        )
                        """);
                sql.execute("PRAGMA user_version=1");
            }
            try (var result = sql.executeQuery("PRAGMA user_version")) {
                if (!result.next() || result.getInt(1) != SCHEMA_VERSION) {
                    throw new SQLException("Missing database migration; refusing a partial upgrade");
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally { connection.setAutoCommit(true); }
    }

    @Override
    public ReaderSettings settings() {
        Map<String, String> values = new HashMap<>();
        try (var sql = connection.createStatement(); var result = sql.executeQuery("SELECT key,value FROM settings")) {
            while (result.next()) values.put(result.getString(1), result.getString(2));
        } catch (SQLException exception) { throw failure(exception); }
        ReaderSettings defaults = ReaderSettings.defaults();
        ViewerTheme theme = enumValue(ViewerTheme.class, values.get("theme"), defaults.theme());
        FitMode fit = enumValue(FitMode.class, values.get("fit_mode"), defaults.fitMode());
        double zoom = defaults.zoom();
        try {
            double parsed = Double.parseDouble(values.getOrDefault("zoom", "1"));
            if (Double.isFinite(parsed) && parsed >= 0.05 && parsed <= 20) zoom = parsed;
        } catch (NumberFormatException ignored) { /* use default for an invalid setting */ }
        return new ReaderSettings(theme, fit, zoom, values.getOrDefault("last_directory", ""), !"false".equals(values.get("thumbnails_visible")));
    }

    @Override
    public void saveSettings(ReaderSettings settings) {
        try {
            connection.setAutoCommit(false);
            try (var sql = connection.prepareStatement("INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
                for (var entry : Map.of("theme", settings.theme().name(), "fit_mode", settings.fitMode().name(),
                        "zoom", Double.toString(settings.zoom()), "last_directory", settings.lastDirectory(),
                        "thumbnails_visible", Boolean.toString(settings.thumbnailsVisible())).entrySet()) {
                    sql.setString(1, entry.getKey()); sql.setString(2, entry.getValue()); sql.addBatch();
                }
                sql.executeBatch();
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        } catch (SQLException exception) { throw failure(exception); }
    }

    @Override
    public Optional<BookState> book(String sourceId) {
        try (var sql = connection.prepareStatement("SELECT * FROM books WHERE source_id=?")) {
            sql.setString(1, sourceId);
            try (var result = sql.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new BookState(sourceId, result.getString("page_id"),
                        result.getInt("page_number"), result.getInt("page_count"), result.getInt("two_page") != 0,
                        enumValue(ReadingMode.class, result.getString("reading_mode"), ReadingMode.MANGA)));
            }
        } catch (SQLException exception) { throw failure(exception); }
    }

    @Override
    public void saveBook(BookState state) {
        try (var sql = connection.prepareStatement("""
                INSERT INTO books(source_id,page_id,page_number,page_count,two_page,reading_mode,updated_at)
                VALUES(?,?,?,?,?,?,?) ON CONFLICT(source_id) DO UPDATE SET
                page_id=excluded.page_id,page_number=excluded.page_number,page_count=excluded.page_count,
                two_page=excluded.two_page,reading_mode=excluded.reading_mode,updated_at=excluded.updated_at
                """)) {
            sql.setString(1, state.sourceId()); sql.setString(2, state.pageId());
            sql.setInt(3, state.pageNumber()); sql.setInt(4, state.pageCount());
            sql.setInt(5, state.twoPageMode() ? 1 : 0); sql.setString(6, state.readingMode().name());
            sql.setLong(7, System.currentTimeMillis()); sql.executeUpdate();
        } catch (SQLException exception) { throw failure(exception); }
    }

    @Override
    public void reset() {
        try {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                sql.executeUpdate("DELETE FROM settings");
                sql.executeUpdate("DELETE FROM books");
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        } catch (SQLException exception) { throw failure(exception); }
    }

    @Override public void close() {
        try { connection.close(); } catch (SQLException exception) { throw failure(exception); }
    }

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Cannot read or save reader state", exception);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try { return value == null ? fallback : Enum.valueOf(type, value); }
        catch (IllegalArgumentException exception) { return fallback; }
    }
}
