package org.dustyroom.be.persistence;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.dustyroom.be.library.LibraryStore;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ReaderSettings;

/**
 * Synchronous API. Call on the startup or session worker thread, never on the EDT.
 */
public interface ReaderStore extends AutoCloseable, LibraryStore {
    static ReaderStore disabled() {
        return new DisabledReaderStore();
    }

    default Path databasePath() {
        return null;
    }

    ReaderSettings settings();

    void saveSettings(ReaderSettings settings);

    Optional<BookState> book(String sourceId);

    void saveBook(BookState state);

    default List<BookState> recentBooks(int limit) {
        return List.of();
    }

    default void hideRecent(String sourceId) {
    }

    default void showRecent(String sourceId) {
    }

    void reset();

    @Override
    void close();
}
