package org.dustyroom.be.persistence;

import lombok.RequiredArgsConstructor;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ReaderSettings;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Session-only reading after a storage failure, with an explicitly confirmed recovery path.
 */
@RequiredArgsConstructor
public final class RecoverableReaderStore implements ReaderStore {
    private final Path database;

    @Override
    public Path databasePath() {
        return database;
    }

    @Override
    public ReaderSettings settings() {
        return ReaderSettings.defaults();
    }

    @Override
    public void saveSettings(ReaderSettings settings) {
        throw new IllegalStateException("The reader database is unavailable. Settings have not been saved.");
    }

    @Override
    public Optional<BookState> book(String sourceId) {
        return Optional.empty();
    }

    @Override
    public void saveBook(BookState state) {
    }

    @Override
    public void reset() {
        DatabaseRecovery.reset(database);
    }

    @Override
    public void close() {
    }
}
