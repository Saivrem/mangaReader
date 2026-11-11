package org.dustyroom.persistence;

import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.persistence.ReaderStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.dustyroom.persistence.PersistenceSql.*;
import static org.junit.jupiter.api.Assertions.*;

class LibraryStoreTest extends BasePersistenceTest {

    private LibraryImport selection() {
        return new LibraryImport(directory.toString(), "夜中", List.of(
                new LibrarySource(directory.resolve("10.zip").toString(), "10.zip", false),
                new LibrarySource(directory.resolve("2.zip").toString(), "2.zip", false)));
    }

    @Test
    void libraryAndHistoryAreIndependentAndReimportPreservesIdentityAndCompletion() throws Exception {
        LibraryImport selection = selection();
        String source = selection.getSources().getFirst().getPath();
        Files.writeString(Path.of(source), "user book must survive");
        String workId;
        String chapterId;
        BookState saved = new BookState(source, "日本語.png", 4, 20, true, ReadingMode.COMICS);
        try (ReaderStore store = openStore()) {
            store.saveBook(saved);
            store.hideRecent(source);
            store.importLibrary(selection);
            LibraryWork work = store.libraryWorks().getFirst();
            workId = work.getId();
            chapterId = work.getChapters().getLast().getId();
            assertEquals(List.of("2.zip", "10.zip"), work.getChapters()
                    .stream()
                    .map(LibraryChapter::getSource)
                    .map(LibrarySource::getTitle)
                    .toList());
            assertEquals(4, work.getResumeChapter().getPage());
            assertTrue(store.recentBooks(12).isEmpty());
            store.finishChapter(chapterId, true);
            store.renameLibrary(workId, "Custom title");
            store.removeLibrary(workId);
            assertTrue(store.libraryWorks().isEmpty());
            assertEquals(saved, store.book(source).orElseThrow());
            store.importLibrary(selection);
            store.importLibrary(selection);
            work = store.libraryWorks().getFirst();
            assertEquals(workId, work.getId());
            assertEquals("Custom title", work.getTitle());
            assertEquals(2, work.getChapters().size());
            assertEquals(chapterId, work.getChapters().getLast().getId());
            assertEquals(1, work.getCompleted());
            assertTrue(store.recentBooks(12).isEmpty());
        }
        try (ReaderStore store = openStore()) {
            assertEquals(workId, store.libraryWorks().getFirst().getId());
            store.reset();
            assertTrue(store.libraryWorks().isEmpty());
            assertTrue(store.book(source).isEmpty());
        }
        assertEquals("user book must survive", Files.readString(Path.of(source)));
    }

    @Test
    void migrationFromSchemaTwoKeepsHiddenStateWithoutCreatingCopies() throws Exception {
        BookState saved = new BookState("old-book", "page", 3, 7, true, ReadingMode.COMICS);
        try (ReaderStore store = openStore()) {
            store.saveBook(saved);
            store.hideRecent(saved.getSourceId());
        }
        execute(DROP_COMPONENTS);
        execute(DROP_CHAPTERS);
        execute(DROP_WORKS);
        setSchemaVersion(2);
        try (ReaderStore store = openStore()) {
            assertEquals(saved, store.book(saved.getSourceId()).orElseThrow());
            assertTrue(store.recentBooks(12).isEmpty());
            assertTrue(store.libraryWorks().isEmpty());
        }
        assertNoBackups();
    }

    @Test
    void transitionalSchemaThreeConvertsWithoutLosingLibraryOrProgress() throws Exception {
        String id;
        BookState saved = new BookState(selection().getSources().getFirst().getPath(), "page", 3, 9, true, ReadingMode.COMICS);
        try (ReaderStore store = openStore()) {
            store.importLibrary(selection());
            store.saveBook(saved);
            store.hideRecent(saved.getSourceId());
            id = store.libraryWorks().getFirst().getId();
            store.finishChapter(store.libraryWorks().getFirst().getChapters().getFirst().getId(), true);
        }
        execute(DROP_COMPONENTS);
        setSchemaVersion(3);
        try (ReaderStore store = openStore()) {
            assertEquals(id, store.libraryWorks().getFirst().getId());
            assertEquals(1, store.libraryWorks().getFirst().getCompleted());
            assertEquals(saved, store.book(saved.getSourceId()).orElseThrow());
            assertTrue(store.recentBooks(12).isEmpty());
        }
        assertEquals(2, scalar(SCHEMA_VERSION));
        assertNoBackups();
    }

    @Test
    void unrecognizedSchemaThreeIsNeverDowngraded() throws Exception {
        execute(CREATE_FUTURE_DATA);
        setSchemaVersion(3);
        assertOpenRejectedWithoutChanges();
    }

    @Test
    void rejectsFutureLibraryVersionWithoutChangingReaderData() throws Exception {
        try (ReaderStore store = openStore()) {
            store.importLibrary(selection());
        }
        execute(UPDATE_COMPONENT, 2, "library");
        assertOpenRejectedWithoutChanges();
    }

    private LibraryImport standalone(String name) {
        String path = directory.resolve(name).toString();
        return new LibraryImport(path, name, List.of(new LibrarySource(path, name, false)));
    }

    @Test
    void overlappingImportsPreserveExistingChapterOwnership() {
        try (ReaderStore store = openStore()) {
            store.importLibrary(selection());
            LibraryWork original = store.libraryWorks().getFirst();
            BookState saved = new BookState(directory.resolve("10.zip").toString(), "page", 3, 9, true, ReadingMode.COMICS);
            store.saveBook(saved);
            store.finishChapter(original.getChapters().getLast().getId(), true);
            store.renameLibrary(original.getId(), "Renamed");
            store.removeLibrary(original.getId());
            List<LibraryImport> batch = List.of(standalone("10.zip"), standalone("new.zip"));
            store.importWorks(batch);
            store.importWorks(batch);
            List<LibraryWork> works = store.libraryWorks();
            assertEquals(2, works.size());
            LibraryWork restored = works
                    .stream()
                    .filter(work -> work.getId().equals(original.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("Renamed", restored.getTitle());
            assertEquals(2, restored.getChapters().size());
            assertEquals(original.getChapters().getLast().getId(), restored.getChapters().getLast().getId());
            assertEquals(1, restored.getCompleted());
            assertEquals(saved, store.book(saved.getSourceId()).orElseThrow());
            assertTrue(works
                    .stream()
                    .anyMatch(work -> work.getTitle().equals("new.zip") && work.getChapters().size() == 1));
        }
    }

    @Test
    void failedImportRollsBackEarlierWorksInTheBatch() throws Exception {
        try (ReaderStore store = openStore()) {
            execute(CREATE_BLOCKED_TITLES);
            execute(BLOCK_TITLE, "bad.zip");
            execute(REJECT_BLOCKED_CHAPTER);
            assertThrows(IllegalStateException.class, () -> store.importWorks(List.of(selection(), standalone("bad.zip"))));
            assertTrue(store.libraryWorks().isEmpty());
        }
    }
}
