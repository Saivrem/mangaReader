package org.dustyroom.ui.loading;

import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ImageIteratorException;
import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.animation.AnimationClip;
import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.book.Picture;
import org.dustyroom.be.models.book.PictureMetadata;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.rendering.SpreadRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AsyncViewerSessionTest {
    private static AsyncViewerSession session(
            FakeImageIterator source,
            ViewerTaskExecutor tasks,
            Executor ui,
            RecordingView view,
            BoundedImageCache cache
    ) {
        return session(file -> source, tasks, ui, view, cache);
    }

    private static AsyncViewerSession session(
            Function<File, ImageIterator> factory,
            ViewerTaskExecutor tasks,
            Executor ui,
            RecordingView view,
            BoundedImageCache cache
    ) {
        return new AsyncViewerSession(
                view,
                factory,
                tasks,
                ui,
                cache,
                new SpreadPlanner(),
                new SpreadRenderer()
        );
    }

    private static BoundedImageCache cache(int entries) {
        return new BoundedImageCache(entries, 1024 * 1024);
    }

    private static ViewerFrame lastFrame(RecordingView view) {
        return view.frames.get(view.frames.size() - 1);
    }

    private static PageRef page(String source, String name) {
        return new PageRef(
                new PageKey(source, name),
                new PictureMetadata(name, source, new File(source))
        );
    }

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void animatedPagesFollowSpreadOrderAndStaleResultsAreDiscarded() {
        FakeImageIterator source = FakeImageIterator.portraits("book", "one.gif", "two.gif", "three.png");
        source.animation = new AnimationClip(() -> {
            throw new IOException("Not opened by session");
        });
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        QueuedExecutor ui = new QueuedExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, ui, view, cache(12));
        session.open(new File("book"));
        tasks.runAll();
        ui.runAll();
        session.toggleTwoPageMode();
        tasks.runAll();
        ui.runAll();
        List<AnimatedPage> manga = view.animations.get(view.animations.size() - 1);
        assertEquals(2, manga.size());
        assertEquals(4, manga.get(0).getX());
        assertEquals(0, manga.get(1).getX());
        session.setComicsReadingMode();
        tasks.runAll();
        ui.runAll();
        List<AnimatedPage> comics = view.animations.get(view.animations.size() - 1);
        assertEquals(0, comics.get(0).getX());
        assertEquals(4, comics.get(1).getX());
        session.first();
        tasks.runAll(); // Animation callback queued, not delivered yet.
        int published = view.animations.size();
        session.goToPage(3);
        tasks.runAll();
        ui.runAll();
        assertEquals(published, view.animations.size());
        assertEquals(3, lastFrame(view).getFirstPage());
        session.close();
        tasks.runAll();
        ui.runAll();
    }

    @Test
    void thumbnailsYieldToNavigationAndDoNotChangeTheDisplayedPage() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two", "three", "four");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer());
        session.open(new File("source"));
        tasks.runAll();
        List<BufferedImage> previews = new ArrayList<>();
        session.thumbnail(3, source.ref("four"), previews::add);
        session.next();
        assertEquals(TaskPriority.INTERACTIVE, tasks.runNext());
        assertEquals(2, lastFrame(view).getFirstPage());
        assertTrue(previews.isEmpty());
        tasks.runAll();
        assertNotNull(previews.get(0));
        assertEquals(2, lastFrame(view).getFirstPage());
        session.close();
        tasks.runAll();
    }

    @Test
    void obsoleteAndFailedThumbnailsDoNotReportReadingErrors() {
        FakeImageIterator old = FakeImageIterator.portraits("old", "one", "two", "bad");
        FakeImageIterator replacement = FakeImageIterator.portraits("new", "one");
        old.failOn("bad");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = new AsyncViewerSession(view,
                file -> file.getName().equals("old") ? old : replacement, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer());
        session.open(new File("old"));
        tasks.runAll();
        List<BufferedImage> previews = new ArrayList<>();
        session.thumbnail(2, old.ref("bad"), previews::add);
        tasks.runAll();
        assertNull(previews.get(0));
        session.thumbnail(2, old.ref("bad"), previews::add);
        session.open(new File("new"));
        tasks.runAll();
        assertNull(previews.get(1));
        assertEquals(1, old.loads("bad"));
        assertTrue(view.errors.isEmpty());
        session.close();
        tasks.runAll();
    }

    @Test
    void restoresStablePageIdentityAndFlushesDisplayedProgressBeforeClosing() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "inserted", "one", "two", "three");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        MemoryStore store = new MemoryStore();
        store.books.put("source", new BookState("source", "two", 2, 3,
                true, ReadingMode.COMICS));
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(new File("source"));
        tasks.runAll();
        assertEquals(3, lastFrame(view).getFirstPage()); // identify page by name, not the old index
        assertTrue(lastFrame(view).isTwoPageMode());
        assertEquals(ReadingMode.COMICS, lastFrame(view).getReadingMode());
        session.remember(lastFrame(view));
        assertEquals(2, store.books.get("source").getPageNumber()); // still queued, no disk work in caller
        session.close();
        tasks.runAll();
        assertEquals(3, store.books.get("source").getPageNumber());
        assertTrue(store.closed);
    }

    @Test
    void missingSavedPageClampsPositionToTheRemainingBook() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        MemoryStore store = new MemoryStore();
        store.books.put("source", new BookState("source", "deleted", 50, 100,
                false, ReadingMode.MANGA));
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(new File("source"));
        tasks.runAll();
        assertEquals(2, lastFrame(view).getFirstPage());
        session.close();
        tasks.runAll();
    }

    @Test
    void explicitImageOverridesSavedFolderPosition(@TempDir Path directory) throws Exception {
        Path selected = directory.resolve("one.png");
        Files.write(selected, new byte[]{0}); // fake decoder below, real file-kind check
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        MemoryStore store = new MemoryStore();
        store.books.put("source", new BookState("source", "two", 2, 2,
                false, ReadingMode.MANGA));
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(selected.toFile());
        tasks.runAll();
        assertEquals(1, lastFrame(view).getFirstPage());
        session.close();
        tasks.runAll();
    }

    @Test
    void storageFailureDoesNotStopReadingAndIsReportedOnce() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        List<ViewerFrame> frames = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ViewerView view = new ViewerView() {
            public void showLoading() {
            }

            public void showFrame(ViewerFrame frame) {
                frames.add(frame);
            }

            public void showError(String error) {
                fail(error);
            }

            public void showPersistenceWarning(String warning) {
                warnings.add(warning);
            }
        };
        MemoryStore store = new MemoryStore() {
            @Override
            public Optional<BookState> book(String id) {
                throw new IllegalStateException("unavailable");
            }

            @Override
            public void saveBook(BookState state) {
                throw new IllegalStateException("read only");
            }
        };
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(new File("source"));
        tasks.runAll();
        session.remember(frames.get(0));
        session.next();
        tasks.runAll();
        assertEquals(2, frames.get(frames.size() - 1).getFirstPage());
        assertEquals(1, warnings.size());
        session.close();
        tasks.runAll();
        assertTrue(store.closed);
    }

    @Test
    void resetDrainsEarlierSavesAndRejectsLateSnapshots() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        MemoryStore store = new MemoryStore();
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(new File("source"));
        tasks.runAll();
        ViewerFrame oldFrame = lastFrame(view);
        session.remember(oldFrame);
        AtomicBoolean finished = new AtomicBoolean();
        session.resetAndClose(() -> finished.set(true), message -> fail(message));
        session.remember(oldFrame);
        session.open(new File("source"));
        tasks.runAll();
        assertTrue(finished.get());
        assertTrue(store.books.isEmpty());
        assertTrue(store.closed);
        assertEquals(1, source.closeCount);
    }

    @Test
    void resetFailureKeepsSessionAvailableForReading() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        MemoryStore store = new MemoryStore() {
            @Override
            public void reset() {
                throw new IllegalStateException("locked");
            }
        };
        AsyncViewerSession session = new AsyncViewerSession(view, file -> source, tasks, Runnable::run,
                cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.open(new File("source"));
        tasks.runAll();
        List<String> errors = new ArrayList<>();
        session.resetAndClose(() -> fail("Must not exit after a failed reset"), errors::add);
        tasks.runAll();
        assertEquals(1, errors.size());
        assertFalse(store.closed);
        session.next();
        tasks.runAll();
        assertEquals(2, lastFrame(view).getFirstPage());
        session.close();
        tasks.runAll();
    }

    @Test
    void staleRecentResultsCannotReappearAfterHideOrReset() {
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        QueuedExecutor ui = new QueuedExecutor();
        List<List<RecentBook>> published = new ArrayList<>();
        MemoryStore store = new MemoryStore();
        store.books.put("missing.zip", new BookState("missing.zip", "p", 2, 5,
                false, ReadingMode.COMICS));
        ViewerView view = new ViewerView() {
            public void showLoading() {
            }

            public void showFrame(ViewerFrame frame) {
            }

            public void showError(String error) {
                fail(error);
            }

            public void showRecentBooks(List<RecentBook> books) {
                published.add(books);
            }
        };
        AsyncViewerSession session = new AsyncViewerSession(view, file -> {
            throw new AssertionError();
        },
                tasks, ui, cache(12), new SpreadPlanner(), new SpreadRenderer(), store);
        session.refreshRecentBooks();
        tasks.runAll(); // Old result is queued for UI delivery.
        session.hideRecentBook("missing.zip");
        tasks.runAll();
        ui.runAll();
        assertFalse(published.isEmpty());
        assertTrue(published.stream().allMatch(List::isEmpty));
        assertTrue(store.book("missing.zip").isPresent());
        store.showRecent("missing.zip");
        session.refreshRecentBooks();
        tasks.runAll();
        int beforeReset = published.size();
        session.resetAndClose(() -> {
        }, error -> fail(error));
        tasks.runAll();
        ui.runAll();
        assertEquals(beforeReset, published.size());
        assertTrue(store.books.isEmpty());
    }

    @Test
    void publishesPageRangesAndModesAndValidatesPageJumps() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "one", "two", "three", "four");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));
        session.open(new File("source"));
        tasks.runAll();
        assertEquals(1, lastFrame(view).getFirstPage());
        assertEquals(1, lastFrame(view).getLastPage());
        assertEquals(4, lastFrame(view).getPageCount());
        assertFalse(lastFrame(view).isTwoPageMode());
        session.goToPage(2);
        session.toggleTwoPageMode();
        session.setComicsReadingMode();
        tasks.runAll();
        assertEquals(2, lastFrame(view).getFirstPage());
        assertEquals(3, lastFrame(view).getLastPage());
        assertTrue(lastFrame(view).isTwoPageMode());
        assertEquals(ReadingMode.COMICS, lastFrame(view).getReadingMode());
        int frameCount = view.frames.size();
        session.goToPage(0);
        tasks.runAll();
        session.goToPage(5);
        tasks.runAll();
        assertEquals(frameCount, view.frames.size());
        assertEquals(2, view.errors.size());
        session.goToPage(4);
        tasks.runAll();
        assertEquals(4, lastFrame(view).getFirstPage());
        assertEquals(4, lastFrame(view).getLastPage());
        session.close();
        tasks.runAll();
    }

    @Test
    void rapidNavigationRunsFifoButPublishesOnlyTheLatestFrame() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2", "page3");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        session.next();
        session.next();

        assertEquals(TaskPriority.INTERACTIVE, tasks.runNext());
        assertEquals(TaskPriority.INTERACTIVE, tasks.runNext());
        assertEquals(TaskPriority.INTERACTIVE, tasks.runNext());
        assertEquals(1, view.frames.size());
        assertTrue(view.frames.get(0).getTitle().contains("page3"));
        assertEquals(1, source.loads("page1"));
        assertEquals(0, source.loads("page2"));
        assertEquals(1, source.loads("page3"));

        assertEquals(TaskPriority.PREFETCH, tasks.runNext());
        session.close();
        tasks.runAll();
    }

    @Test
    void queuedUiCallbacksFromAnOlderRequestAreIgnored() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        QueuedExecutor ui = new QueuedExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, ui, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        session.next();
        ui.runAll();
        assertTrue(view.frames.isEmpty());

        tasks.runNext();
        ui.runAll();
        assertEquals(1, view.frames.size());
        assertTrue(view.frames.get(0).getTitle().contains("page2"));

        session.close();
        tasks.runAll();
        ui.runAll();
    }

    @Test
    void replacingSourceClosesTheOldSourceAndUsesTheNewGeneration() {
        FakeImageIterator first = FakeImageIterator.portraits("first", "old");
        FakeImageIterator second = FakeImageIterator.portraits("second", "new");
        Map<String, FakeImageIterator> sources = Map.of("first", first, "second", second);
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(
                file -> sources.get(file.getPath()),
                tasks,
                Runnable::run,
                view,
                cache(12)
        );

        session.open(new File("first"));
        tasks.runNext();
        session.open(new File("second"));
        tasks.runNext();

        assertEquals(1, first.closeCount);
        assertEquals(0, second.closeCount);
        assertTrue(view.frames.get(view.frames.size() - 1).getTitle().contains("new"));

        session.close();
        tasks.runAll();
        assertEquals(1, second.closeCount);
    }

    @Test
    void reopeningTheSameFactoryInstanceDoesNotCloseTheActiveSource() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        session.open(new File("source"));
        tasks.runNext();

        assertEquals(0, source.closeCount);
        session.next();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().endsWith("page2"));

        session.close();
        tasks.runAll();
        assertEquals(1, source.closeCount);
    }

    @Test
    void failedReopenOfTheSameFactoryInstanceLeavesItUsable() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        source.failOn("page1");
        session.open(new File("source"));
        tasks.runNext();

        assertEquals(0, source.closeCount);
        assertEquals(List.of("Failed to decode page1"), view.errors);
        session.next();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().endsWith("page2"));

        session.close();
        tasks.runAll();
        assertEquals(1, source.closeCount);
    }

    @Test
    void failedInitialDecodeKeepsThePreviousSourceAndCacheUsable() {
        FakeImageIterator first = FakeImageIterator.portraits("first", "old1", "old2");
        FakeImageIterator broken = FakeImageIterator.portraits("broken", "broken-page");
        broken.failOn("broken-page");
        Map<String, FakeImageIterator> sources = Map.of("first", first, "broken", broken);
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(
                file -> sources.get(file.getPath()),
                tasks,
                Runnable::run,
                view,
                cache(12)
        );

        session.open(new File("first"));
        tasks.runNext();
        session.open(new File("broken"));
        tasks.runNext();

        assertEquals(0, first.closeCount);
        assertEquals(1, broken.closeCount);
        assertEquals(List.of("Failed to decode broken-page"), view.errors);
        assertTrue(lastFrame(view).getTitle().endsWith("old1"));

        session.first();
        tasks.runNext();
        assertEquals(1, first.loads("old1"));

        session.next();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().endsWith("old2"));

        session.close();
        tasks.runAll();
        assertEquals(1, first.closeCount);
    }

    @Test
    void navigationQueuedAfterFailedOpenPublishesTheSourceError() {
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(
                file -> {
                    throw new ImageIteratorException("Broken source", file);
                },
                tasks,
                Runnable::run,
                view,
                cache(12)
        );

        session.open(new File("broken"));
        session.next();
        tasks.runAll();

        assertEquals(List.of("Broken source"), view.errors);
        assertTrue(view.frames.isEmpty());

        session.close();
        tasks.runAll();
    }

    @Test
    void portraitSpreadAdvancesByTwoAndPreviousRestoresItsAnchor() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2", "page3");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        session.toggleTwoPageMode();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().contains("page1 + page2"));

        session.next();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().endsWith("page3"));

        session.previous();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().contains("page1 + page2"));

        session.close();
        tasks.runAll();
    }

    @Test
    void landscapePageRemainsSingleInTwoPageMode() {
        FakeImageIterator source = new FakeImageIterator(
                "source",
                List.of(page("source", "landscape"), page("source", "portrait")),
                Map.of(
                        "landscape", image(8, 4),
                        "portrait", image(4, 8)
                )
        );
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        session.toggleTwoPageMode();
        tasks.runNext();
        assertFalse(lastFrame(view).getTitle().contains("+"));

        session.next();
        tasks.runNext();
        assertTrue(lastFrame(view).getTitle().endsWith("portrait"));

        session.close();
        tasks.runAll();
    }

    @Test
    void evictionReloadsAReferencedPageWithoutKeepingImagesInHistory() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        BoundedImageCache cache = cache(1);
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache);

        session.open(new File("source"));
        tasks.runNext();
        tasks.runNext(); // Prefetch page2 and evict page1.
        assertEquals(1, source.loads("page1"));

        session.next();
        tasks.runNext();
        session.previous();
        tasks.runNext();

        assertEquals(2, source.loads("page1"));
        assertTrue(lastFrame(view).getTitle().endsWith("page1"));

        session.close();
        tasks.runAll();
    }

    @Test
    void decodingFailureKeepsTheLastGoodFrameAndRollsBackNavigation() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "good", "broken");
        source.failOn("broken");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        assertEquals(1, view.frames.size());

        session.next();
        tasks.runNext();
        assertEquals(1, view.frames.size());
        assertEquals(1, view.errors.size());

        session.first();
        tasks.runNext();
        assertEquals(2, view.frames.size());
        assertTrue(lastFrame(view).getTitle().endsWith("good"));

        session.close();
        tasks.runAll();
    }

    @Test
    void prefetchWarmsCacheWithoutPublishingAFrame() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        BoundedImageCache cache = cache(12);
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache);

        session.open(new File("source"));
        tasks.runNext();
        assertEquals(1, view.frames.size());
        assertFalse(cache.contains(source.ref("page2").getKey()));

        assertEquals(TaskPriority.PREFETCH, tasks.runNext());
        assertTrue(cache.contains(source.ref("page2").getKey()));
        assertEquals(1, view.frames.size());

        session.close();
        tasks.runAll();
    }

    @Test
    void stalePrefetchDoesNotLoadAnOldAnchorTarget() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1", "page2", "page3");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));

        session.open(new File("source"));
        tasks.runNext(); // Queues prefetch for page2.
        session.next();
        tasks.runNext(); // Renders page2 and queues prefetch for page3.

        assertEquals(TaskPriority.PREFETCH, tasks.runNext());
        assertEquals(1, source.loads("page2"));
        assertEquals(0, source.loads("page3"));

        assertEquals(TaskPriority.PREFETCH, tasks.runNext());
        assertEquals(1, source.loads("page3"));

        session.close();
        tasks.runAll();
    }

    @Test
    void closeIsQueuedAfterWorkAndClosesSourceExactlyOnce() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        QueuedExecutor ui = new QueuedExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, ui, view, cache(12));

        session.open(new File("source"));
        tasks.runNext();
        session.close();
        session.close();
        tasks.runAll();
        ui.runAll();

        assertEquals(1, source.closeCount);
        assertTrue(view.frames.isEmpty());
    }

    @Test
    void multipleCloseCallbacksRunInRegistrationOrderAfterCleanup() {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1");
        ManualTaskExecutor tasks = new ManualTaskExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));
        List<String> callbacks = new ArrayList<>();

        session.open(new File("source"));
        tasks.runNext();
        session.close(() -> callbacks.add("first"));
        session.close(() -> callbacks.add("second"));

        assertTrue(callbacks.isEmpty());
        assertEquals(0, source.closeCount);
        tasks.runNext();
        assertEquals(1, source.closeCount);
        assertEquals(List.of("first", "second"), callbacks);

        session.close(() -> callbacks.add("third"));
        assertEquals(List.of("first", "second", "third"), callbacks);
        tasks.runAll();
    }

    @Test
    void closeCallbackWaitsForAnInFlightInitialDecode() throws Exception {
        FakeImageIterator source = FakeImageIterator.portraits("source", "page1");
        source.blockOn("page1");
        SerialPriorityExecutor tasks = new SerialPriorityExecutor();
        RecordingView view = new RecordingView();
        AsyncViewerSession session = session(source, tasks, Runnable::run, view, cache(12));
        CountDownLatch closeCallback = new CountDownLatch(1);

        session.open(new File("source"));
        assertTrue(source.awaitBlockedLoad());
        session.close(closeCallback::countDown);

        assertFalse(closeCallback.await(100, TimeUnit.MILLISECONDS));
        source.releaseBlockedLoad();
        assertTrue(closeCallback.await(2, TimeUnit.SECONDS));
        assertEquals(1, source.closeCount);
        assertTrue(view.frames.isEmpty());
    }

    private static class MemoryStore implements ReaderStore {
        final Map<String, BookState> books = new HashMap<>();
        final Set<String> hidden = new HashSet<>();
        boolean closed;

        public List<BookState> recentBooks(int limit) {
            assertFalse(closed);
            return books.values().stream().filter(book -> !hidden.contains(book.getSourceId())).limit(limit).toList();
        }

        public void hideRecent(String id) {
            hidden.add(id);
        }

        public void showRecent(String id) {
            hidden.remove(id);
        }

        public ReaderSettings settings() {
            return ReaderSettings.defaults();
        }

        public void saveSettings(ReaderSettings settings) {
        }

        public Optional<BookState> book(String id) {
            return Optional.ofNullable(books.get(id));
        }

        public void saveBook(BookState state) {
            assertFalse(closed);
            books.put(state.getSourceId(), state);
        }

        public void reset() {
            books.clear();
        }

        public void close() {
            closed = true;
        }
    }

    private static final class RecordingView implements ViewerView {
        private final List<ViewerFrame> frames = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<List<AnimatedPage>> animations = new ArrayList<>();
        private int loadingCount;

        @Override
        public void showAnimations(PageKey page, List<AnimatedPage> pages) {
            animations.add(pages);
        }

        @Override
        public void showLoading() {
            loadingCount++;
        }

        @Override
        public void showFrame(ViewerFrame frame) {
            frames.add(frame);
        }

        @Override
        public void showError(String message) {
            errors.add(message);
        }
    }

    private static final class QueuedExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runAll() {
            List<Runnable> queued = new ArrayList<>(tasks);
            tasks.clear();
            queued.forEach(Runnable::run);
        }
    }

    private static final class ManualTaskExecutor implements ViewerTaskExecutor {
        private final AtomicLong sequence = new AtomicLong();
        private final List<QueuedTask> tasks = new ArrayList<>();
        private final List<TaskPriority> executionOrder = new ArrayList<>();
        private boolean closed;

        @Override
        public boolean execute(TaskPriority priority, Runnable task) {
            if (closed) {
                return false;
            }
            tasks.add(new QueuedTask(priority, sequence.getAndIncrement(), task));
            return true;
        }

        TaskPriority runNext() {
            QueuedTask next = tasks.stream()
                    .min(Comparator.comparing(QueuedTask::getPriority).thenComparingLong(QueuedTask::getSequence))
                    .orElseThrow();
            tasks.remove(next);
            executionOrder.add(next.getPriority());
            next.getTask().run();
            return next.getPriority();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                runNext();
            }
        }

        @Override
        public void close() {
            closed = true;
        }

    }

    private static final class FakeImageIterator implements ImageIterator {
        private final String sourceId;
        private final List<PageRef> pages;
        private final Map<String, BufferedImage> images;
        private final Map<String, Integer> loadCounts = new HashMap<>();
        private final List<String> failures = new ArrayList<>();
        private AnimationClip animation;
        private volatile String blockedPage;
        private volatile CountDownLatch blockedLoadStarted;
        private volatile CountDownLatch releaseBlockedLoad;
        private int cursor;
        private int closeCount;

        private FakeImageIterator(String sourceId, List<PageRef> pages, Map<String, BufferedImage> images) {
            this.sourceId = sourceId;
            this.pages = pages;
            this.images = images;
        }

        static FakeImageIterator portraits(String source, String... names) {
            List<PageRef> pages = new ArrayList<>();
            Map<String, BufferedImage> images = new HashMap<>();
            for (String name : names) {
                pages.add(page(source, name));
                images.put(name, image(4, 8));
            }
            return new FakeImageIterator(source, List.copyOf(pages), images);
        }

        @Override
        public AnimationClip animation(PageRef page) {
            return animation;
        }

        PageRef ref(String name) {
            return pages.stream().filter(page -> page.getKey().getPageId().equals(name)).findFirst().orElseThrow();
        }

        void failOn(String name) {
            failures.add(name);
        }

        void blockOn(String name) {
            blockedPage = name;
            blockedLoadStarted = new CountDownLatch(1);
            releaseBlockedLoad = new CountDownLatch(1);
        }

        boolean awaitBlockedLoad() throws InterruptedException {
            CountDownLatch started = blockedLoadStarted;
            return started != null && started.await(2, TimeUnit.SECONDS);
        }

        void releaseBlockedLoad() {
            CountDownLatch release = releaseBlockedLoad;
            if (release != null) {
                release.countDown();
            }
        }

        int loads(String name) {
            return loadCounts.getOrDefault(name, 0);
        }

        @Override
        public void init() {
        }

        @Override
        public File getVolumeRoot() {
            return new File(sourceId);
        }

        @Override
        public void setFile(File file) {
        }

        @Override
        public Picture next() {
            Picture picture = load(pages.get(cursor));
            cursor = Math.floorMod(cursor + 1, pages.size());
            return picture;
        }

        @Override
        public Picture prev() {
            cursor = Math.floorMod(cursor - 1, pages.size());
            return load(pages.get(cursor));
        }

        @Override
        public Picture first() {
            cursor = 0;
            return load(pages.get(cursor));
        }

        @Override
        public Picture last() {
            cursor = pages.size() - 1;
            return load(pages.get(cursor));
        }

        @Override
        public Picture nextVol() {
            return first();
        }

        @Override
        public Picture prevVol() {
            return first();
        }

        @Override
        public List<PageRef> pages() {
            return pages;
        }

        @Override
        public int initialPageIndex() {
            return 0;
        }

        @Override
        public Picture load(PageRef page) {
            String name = page.getKey().getPageId();
            loadCounts.merge(name, 1, Integer::sum);
            CountDownLatch started = blockedLoadStarted;
            if (name.equals(blockedPage) && started != null) {
                blockedPage = null;
                started.countDown();
                try {
                    if (!releaseBlockedLoad.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to release blocked load");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Blocked load was interrupted", e);
                }
            }
            if (failures.contains(name)) {
                throw new ImageIteratorException("Failed to decode " + name, new File(sourceId));
            }
            return new Picture(images.get(name), page.getMetadata());
        }

        @Override
        public PageRef switchToNextVolume() {
            return pages.get(0);
        }

        @Override
        public PageRef switchToPreviousVolume() {
            return pages.get(0);
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
