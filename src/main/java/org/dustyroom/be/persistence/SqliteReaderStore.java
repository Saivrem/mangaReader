package org.dustyroom.be.persistence;

import org.dustyroom.be.utils.SqliteUtils;
import org.dustyroom.be.persistence.schema.SqlitePragmas;
import org.apache.commons.lang3.EnumUtils;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.persistence.schema.LibrarySchema;
import org.dustyroom.be.persistence.schema.ReaderSchema;
import org.dustyroom.be.persistence.schema.ReaderSql;
import org.dustyroom.be.settings.SettingsCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

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
            opened = SqliteUtils.connect(path);
            connection = opened;
            int version;
            try (Statement sql = connection.createStatement()) {
                sql.execute(SqlitePragmas.BUSY_TIMEOUT);
                try (ResultSet result = sql.executeQuery(SqlitePragmas.READ_VERSION)) {
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
                    SqliteUtils.addBatch(sql, entry.getKey(), entry.getValue());
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
        try (PreparedStatement sql = SqliteUtils.prepare(connection, ReaderSql.SELECT_BOOK, sourceId);
             ResultSet result = sql.executeQuery()) {
            if (!result.next()) {
                return Optional.empty();
            }
            return Optional.of(readBook(result, sourceId));
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static BookState readBook(ResultSet row, String sourceId) throws SQLException {
        ReadingMode mode = EnumUtils.getEnum(ReadingMode.class, row.getString("reading_mode"), ReadingMode.MANGA);
        return new BookState(sourceId, row.getString("page_id"), row.getInt("page_number"),
                row.getInt("page_count"), row.getBoolean("two_page"), mode);
    }

    @Override
    public void saveBook(BookState state) {
        try {
            SqliteUtils.update(connection, ReaderSql.SAVE_BOOK,
                    state.getSourceId(),
                    state.getPageId(),
                    state.getPageNumber(),
                    state.getPageCount(),
                    state.isTwoPageMode() ? 1 : 0,
                    state.getReadingMode().name(),
                    System.currentTimeMillis());
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public List<BookState> recentBooks(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Invalid recent book limit");
        }
        List<BookState> books = new ArrayList<>();
        try (PreparedStatement sql = SqliteUtils.prepare(connection, ReaderSql.SELECT_RECENTS, limit);
             ResultSet result = sql.executeQuery()) {
            while (result.next()) {
                books.add(readBook(result, result.getString("source_id")));
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
        try {
            SqliteUtils.update(connection, ReaderSql.SET_RECENT_HIDDEN, hidden ? 1 : 0, sourceId);
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
    public void importWorks(List<LibraryImport> selections) {
        new SqliteLibraryStore(connection).importWorks(selections);
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
