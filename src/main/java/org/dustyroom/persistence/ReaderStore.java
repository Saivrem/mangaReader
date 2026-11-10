package org.dustyroom.persistence;

import java.util.List;
import java.util.Optional;

/**
 * Synchronous API. Call on the startup or session worker thread, never on the EDT.
 */
public interface ReaderStore extends AutoCloseable {
    static ReaderStore disabled() {
        return new ReaderStore() {
            public ReaderSettings settings() {
                return ReaderSettings.defaults();
            }

            public void saveSettings(ReaderSettings settings) {
            }

            public Optional<BookState> book(String id) {
                return Optional.empty();
            }

            public void saveBook(BookState state) {
            }

            public void reset() {
                throw new IllegalStateException("Persistent storage is unavailable; the database was not reset.");
            }

            public void close() {
            }
        };
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
