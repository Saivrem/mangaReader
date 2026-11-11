package org.dustyroom.be.persistence;

import org.dustyroom.be.utils.SqliteUtils;
import lombok.RequiredArgsConstructor;
import org.dustyroom.be.library.LibraryStore;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.persistence.schema.LibrarySql;
import org.dustyroom.be.utils.NaturalOrderComparator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shares the reader connection and its serial worker; never owns its lifecycle.
 */
@RequiredArgsConstructor
final class SqliteLibraryStore implements LibraryStore {
    private final Connection connection;

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Cannot read or save the library. Your files were not changed.", exception);
    }

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
        try (PreparedStatement sql = SqliteUtils.prepare(connection, LibrarySql.SELECT_CHAPTERS, work);
             ResultSet rows = sql.executeQuery()) {
            while (rows.next()) {
                chapters.add(new LibraryChapter(rows.getString("id"),
                        new LibrarySource(rows.getString("source_path"), rows.getString("title"), rows.getBoolean("directory")),
                        rows.getBoolean("finished"), rows.getInt("page_number"), rows.getInt("page_count"), rows.getLong("updated_at")));
            }
        }
        chapters.sort((left, right) -> NaturalOrderComparator.INSTANCE.compare(left.getSource().getTitle(), right.getSource().getTitle()));
        return List.copyOf(chapters);
    }

    @Override
    public void importWorks(List<LibraryImport> selections) {
        for (LibraryImport selection : selections) {
            if (selection.getSources().isEmpty() || selection.getTitle().isBlank()) {
                throw new IllegalArgumentException("Select at least one chapter and a title.");
            }
        }
        try {
            connection.setAutoCommit(false);
            try {
                for (LibraryImport selection : selections) {
                    importWork(selection);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private void importWork(LibraryImport selection) throws SQLException {
        List<LibrarySource> newSources = new ArrayList<>();
        try (PreparedStatement owner = connection.prepareStatement(LibrarySql.CHAPTER_OWNER);
             PreparedStatement restore = connection.prepareStatement(LibrarySql.RESTORE_WORK)) {
            for (LibrarySource source : selection.getSources()) {
                owner.setString(1, source.getPath());
                try (ResultSet row = owner.executeQuery()) {
                    if (row.next()) {
                        SqliteUtils.addBatch(restore, row.getString(1));
                    } else {
                        newSources.add(source);
                    }
                }
            }
            restore.executeBatch();
        }
        if (newSources.isEmpty()) {
            return;
        }
        String work = UUID.randomUUID().toString();
        SqliteUtils.update(connection, LibrarySql.IMPORT_WORK,
                work, selection.getRoot(), selection.getTitle().strip(), System.currentTimeMillis());

        try (PreparedStatement sql = SqliteUtils.prepare(connection, LibrarySql.FIND_WORK, selection.getRoot());
             ResultSet row = sql.executeQuery()) {
            row.next();
            work = row.getString(1);
        }
        try (PreparedStatement sql = connection.prepareStatement(LibrarySql.IMPORT_CHAPTER)) {
            for (LibrarySource source : newSources) {
                SqliteUtils.addBatch(sql, UUID.randomUUID().toString(),
                        work,
                        source.getPath(),
                        source.getTitle(),
                        source.isDirectory());
            }
            sql.executeBatch();
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
        update(LibrarySql.RENAME_WORK, title.strip(), id);
    }

    @Override
    public void finishChapter(String id, boolean finished) {
        update(LibrarySql.SET_CHAPTER_FINISHED, finished, id);
    }

    private void update(String statement, Object... values) {
        try {
            SqliteUtils.update(connection, statement, values);
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }
}
