package org.dustyroom.be.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.persistence.schema.LibrarySchema;
import org.dustyroom.be.persistence.schema.ReaderSchema;
import org.dustyroom.be.persistence.schema.ReaderSql;
import org.dustyroom.be.settings.SettingsCodec;

public final class SqliteReaderStore implements ReaderStore {
    // Independent of the application release version (0.8, 0.9, ...).
    static final int SCHEMA_VERSION = ReaderSchema.VERSION;
    private final Connection connection;
    private final Path database;

    public SqliteReaderStore(Path database) {
        this.database = database.toAbsolutePath().normalize();
        Connection opened = null;
        try {
            Path path = database.toAbsolutePath().normalize();
            Files.createDirectories(path.getParent());
            opened = DriverManager.getConnection("jdbc:sqlite:" + path);
            connection = opened;
            int version;
            try (Statement sql = connection.createStatement()) {
                sql.execute(ReaderSql.BUSY_TIMEOUT);
                try (ResultSet result = sql.executeQuery(ReaderSql.READ_VERSION)) {
                    version = result.next() ? result.getInt(1) : 0;

                }
            }
            boolean transitional = version == 3 && LibrarySchema.transitionalSchema(connection);
            if (version > SCHEMA_VERSION && !transitional) {
                throw new SQLException("Database schema is newer than this application; no changes were made");
            }
            int libraryVersion = LibrarySchema.version(connection);
            if (libraryVersion > LibrarySchema.VERSION || libraryVersion < 0) {
                throw new SQLException("Library schema is newer than this application; no changes were made");
            }
            ReaderSchema.migrate(connection, version, libraryVersion, transitional);
            try (Statement sql = connection.createStatement()) {
                sql.execute(ReaderSql.ENABLE_WAL);
            }
        } catch (Exception exception) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (SQLException closeError) {
                    exception.addSuppressed(closeError);
                }
            }
            throw new IllegalStateException("Cannot open reader database: " + database, exception);
        }
    }

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Cannot read or save reader state", exception);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return value == null ? fallback : Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    @Override
    public Path databasePath() {
        return database;
    }

    @Override
    public ReaderSettings settings() {
        Map<String, String> values = new HashMap<>();
        try (Statement sql = connection.createStatement(); ResultSet result = sql.executeQuery(ReaderSql.SELECT_SETTINGS)) {
            while (result.next()) {
                values.put(result.getString(1), result.getString(2));
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
        return SettingsCodec.decode(values);
    }

    @Override
    public void saveSettings(ReaderSettings settings) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement sql = connection.prepareStatement(ReaderSql.SAVE_SETTING)) {
                for (Map.Entry<String, String> entry : SettingsCodec.encode(settings).entrySet()) {
                    sql.setString(1, entry.getKey());
                    sql.setString(2, entry.getValue());
                    sql.addBatch();
                }
                sql.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Optional<BookState> book(String sourceId) {
        try (PreparedStatement sql = connection.prepareStatement(ReaderSql.SELECT_BOOK)) {
            sql.setString(1, sourceId);
            try (ResultSet result = sql.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BookState(sourceId, result.getString("page_id"),
                        result.getInt("page_number"), result.getInt("page_count"), result.getInt("two_page") != 0,
                        enumValue(ReadingMode.class, result.getString("reading_mode"), ReadingMode.MANGA)));
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void saveBook(BookState state) {
        try (PreparedStatement sql = connection.prepareStatement(ReaderSql.SAVE_BOOK)) {
            sql.setString(1, state.getSourceId());
            sql.setString(2, state.getPageId());
            sql.setInt(3, state.getPageNumber());
            sql.setInt(4, state.getPageCount());
            sql.setInt(5, state.isTwoPageMode() ? 1 : 0);
            sql.setString(6, state.getReadingMode().name());
            sql.setLong(7, System.currentTimeMillis());
            sql.executeUpdate();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<BookState> recentBooks(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Invalid recent book limit");
        }
        List<BookState> books = new ArrayList<BookState>();
        try (PreparedStatement sql = connection.prepareStatement(ReaderSql.SELECT_RECENTS)) {
            sql.setInt(1, limit);
            try (ResultSet result = sql.executeQuery()) {
                while (result.next()) {
                    books.add(new BookState(result.getString("source_id"), result.getString("page_id"),
                            result.getInt("page_number"), result.getInt("page_count"), result.getInt("two_page") != 0,
                            enumValue(ReadingMode.class, result.getString("reading_mode"), ReadingMode.MANGA)));
                }
            }
            return List.copyOf(books);
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void hideRecent(String sourceId) {
        setRecentHidden(sourceId, true);
    }

    @Override
    public void showRecent(String sourceId) {
        setRecentHidden(sourceId, false);
    }

    private void setRecentHidden(String sourceId, boolean hidden) {
        try (PreparedStatement sql = connection.prepareStatement(ReaderSql.SET_RECENT_HIDDEN)) {
            sql.setInt(1, hidden ? 1 : 0);
            sql.setString(2, sourceId);
            sql.executeUpdate();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void reset() {
        try {
            connection.setAutoCommit(false);
            try (Statement sql = connection.createStatement()) {
                sql.executeUpdate(ReaderSql.CLEAR_CHAPTERS);
                sql.executeUpdate(ReaderSql.CLEAR_WORKS);
                sql.executeUpdate(ReaderSql.CLEAR_SETTINGS);
                sql.executeUpdate(ReaderSql.CLEAR_BOOKS);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<LibraryWork> libraryWorks() {
        return new SqliteLibraryStore(connection).libraryWorks();
    }
    @Override
    public void importLibrary(LibraryImport selection) {
        new SqliteLibraryStore(connection).importLibrary(selection);
    }
    @Override
    public void removeLibrary(String id) {
        new SqliteLibraryStore(connection).removeLibrary(id);
    }
    @Override
    public void renameLibrary(String id, String title) {
        new SqliteLibraryStore(connection).renameLibrary(id, title);
    }
    @Override
    public void finishChapter(String id, boolean finished) {
        new SqliteLibraryStore(connection).finishChapter(id, finished);
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }
}
