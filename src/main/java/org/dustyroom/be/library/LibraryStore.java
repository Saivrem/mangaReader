package org.dustyroom.be.library;

import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibraryWork;

import java.util.List;

/**
 * Synchronous storage; use the reader's serial worker.
 */
public interface LibraryStore {
    private static IllegalStateException unavailable() {
        return new IllegalStateException("Library storage is unavailable. Your books were not changed.");
    }

    default List<LibraryWork> libraryWorks() {
        return List.of();
    }

    default void importLibrary(LibraryImport selection) {
        importWorks(List.of(selection));
    }

    default void importWorks(List<LibraryImport> selections) {
        throw unavailable();
    }

    default void removeLibrary(String id) {
        throw unavailable();
    }

    default void renameLibrary(String id, String title) {
        throw unavailable();
    }

    default void finishChapter(String id, boolean finished) {
        throw unavailable();
    }
}
