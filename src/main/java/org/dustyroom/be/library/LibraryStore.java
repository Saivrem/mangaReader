package org.dustyroom.be.library;

import java.util.List;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibraryWork;

/** Synchronous storage; use the reader's serial worker. */
public interface LibraryStore {
    default List<LibraryWork> libraryWorks() {
        return List.of();
    }
    default void importLibrary(LibraryImport selection) {
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

    private static IllegalStateException unavailable() {
        return new IllegalStateException("Library storage is unavailable. Your books were not changed.");
    }
}
