package org.dustyroom.be.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.dustyroom.be.library.LibraryStore;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.persistence.schema.LibrarySql;
import org.dustyroom.be.utils.NaturalOrderComparator;

/** Shares the reader connection and its serial worker; never owns its lifecycle. */
@RequiredArgsConstructor
final class SqliteLibraryStore implements LibraryStore {
    private final Connection connection;

    @Override
    public List<LibraryWork> libraryWorks() {
        List<LibraryWork> works = new ArrayList<>();
        try (PreparedStatement sql = connection.prepareStatement(LibrarySql.SELECT_WORKS);
             ResultSet rows = sql.executeQuery()) {
            while (rows.next()) {
                String id = rows.getString("id");
                works.add(new LibraryWork(id, rows.getString("title"), rows.getString("root"),
                        rows.getLong("added_at"), chapters(id)));
            }
            return List.copyOf(works);
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private List<LibraryChapter> chapters(String work) throws SQLException {
        List<LibraryChapter> chapters = new ArrayList<>();
        try (PreparedStatement sql = connection.prepareStatement(LibrarySql.SELECT_CHAPTERS)) {
            sql.setString(1, work);
            try (ResultSet rows = sql.executeQuery()) {
                while (rows.next()) {
                    chapters.add(new LibraryChapter(rows.getString("id"),
                        new LibrarySource(rows.getString("source_path"), rows.getString("title"), rows.getBoolean("directory")),
                        rows.getBoolean("finished"), rows.getInt("page_number"), rows.getInt("page_count"), rows.getLong("updated_at")));
                }
            }
        }
        chapters.sort((left, right) -> NaturalOrderComparator.INSTANCE.compare(left.getSource().getTitle(), right.getSource().getTitle()));
        return List.copyOf(chapters);
    }

    @Override
    public void importLibrary(LibraryImport selection) {
        if (selection.getSources().isEmpty() || selection.getTitle().isBlank()) {
            throw new IllegalArgumentException("Select at least one chapter and a title.");
        }
        try {
            connection.setAutoCommit(false);
            try {
                String work = UUID.randomUUID().toString();
                try (PreparedStatement sql = connection.prepareStatement(LibrarySql.IMPORT_WORK)) {
                    sql.setString(1, work);
                    sql.setString(2, selection.getRoot());
                    sql.setString(3, selection.getTitle().strip());
                    sql.setLong(4, System.currentTimeMillis());
                    sql.executeUpdate();
                }
                try (PreparedStatement sql = connection.prepareStatement(LibrarySql.FIND_WORK)) {
                    sql.setString(1, selection.getRoot());
                    try (ResultSet row = sql.executeQuery()) {
                        row.next();
                        work = row.getString(1);
                    }
                }
                try (PreparedStatement sql = connection.prepareStatement(LibrarySql.IMPORT_CHAPTER)) {
                    for (LibrarySource source : selection.getSources()) {
                        sql.setString(1, UUID.randomUUID().toString());
                        sql.setString(2, work);
                        sql.setString(3, source.getPath());
                        sql.setString(4, source.getTitle());
                        sql.setBoolean(5, source.isDirectory());
                        sql.addBatch();
                    }
                    sql.executeBatch();
                }
                if (chapters(work).isEmpty()) {
                    throw new SQLException("The selected sources already belong to another work.");
                }
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
    public void removeLibrary(String id) {
        update(LibrarySql.REMOVE_WORK, id);
    }

    @Override
    public void renameLibrary(String id, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A title is required.");
        }
        try (PreparedStatement sql = connection.prepareStatement(LibrarySql.RENAME_WORK)) {
            sql.setString(1, title.strip());
            sql.setString(2, id);
            sql.executeUpdate();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void finishChapter(String id, boolean finished) {
        try (PreparedStatement sql = connection.prepareStatement(LibrarySql.SET_CHAPTER_FINISHED)) {
            sql.setBoolean(1, finished);
            sql.setString(2, id);
            sql.executeUpdate();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private void update(String statement, String id) {
        try (PreparedStatement sql = connection.prepareStatement(statement)) {
            sql.setString(1, id);
            sql.executeUpdate();
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Cannot read or save the library. Your files were not changed.", exception);
    }
}
