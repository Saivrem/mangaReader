package org.dustyroom.ui.loading;

import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ImageIteratorFactory;
import org.dustyroom.be.models.PageKey;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.PageShape;
import org.dustyroom.be.models.Picture;
import org.dustyroom.persistence.BookState;
import org.dustyroom.persistence.ReaderSettings;
import org.dustyroom.persistence.ReaderStore;
import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.rendering.SpreadRenderer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.dustyroom.ui.loading.TaskPriority.INTERACTIVE;
import static org.dustyroom.ui.loading.TaskPriority.PREFETCH;
import static org.dustyroom.ui.navigation.ReadingMode.COMICS;
import static org.dustyroom.ui.navigation.ReadingMode.MANGA;

public final class AsyncViewerSession implements AutoCloseable {
    static final int DEFAULT_CACHE_ENTRIES = 12;
    static final long DEFAULT_CACHE_BYTES = 256L * 1024 * 1024;

    private final ViewerView view;
    private final ReaderStore store;
    private final Function<File, ImageIterator> sourceFactory;
    private final ViewerTaskExecutor tasks;
    private final Executor uiExecutor;
    private final BoundedImageCache cache;
    private final SpreadPlanner spreadPlanner;
    private final SpreadRenderer spreadRenderer;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong requestSequence = new AtomicLong();
    private final AtomicLong latestRequest = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object closeMonitor = new Object();
    private final List<Runnable> closeCallbacks = new ArrayList<>();
    private final Map<PageKey, PageShape> shapes = new HashMap<>();
    private final AtomicLong recentRequest = new AtomicLong();
    private boolean persistenceWarningShown;
    private boolean cleanupComplete;
    private boolean closeCallbackDrainScheduled;
    // The following state is owned by the serial task executor.
    private ImageIterator source;
    private List<PageRef> pages = List.of();
    private int pageIndex = -1;
    private boolean twoPageMode;
    private ReadingMode readingMode = MANGA;
    private String lastSourceError;
    private String lastRememberedSource;
    private volatile boolean groupRecentByFolder = true;
    private volatile List<RecentBook> shownRecent = List.of();

    public AsyncViewerSession(ViewerView view) {
        this(view, ReaderStore.disabled());
    }

    public AsyncViewerSession(ViewerView view, ReaderStore store) {
        this(
                view,
                ImageIteratorFactory::create,
                new SerialPriorityExecutor(),
                SwingUtilities::invokeLater,
                new BoundedImageCache(DEFAULT_CACHE_ENTRIES, DEFAULT_CACHE_BYTES),
                new SpreadPlanner(),
                new SpreadRenderer(),
                store
        );
    }

    public AsyncViewerSession(
            ViewerView view,
            Function<File, ImageIterator> sourceFactory,
            ViewerTaskExecutor tasks,
            Executor uiExecutor,
            BoundedImageCache cache,
            SpreadPlanner spreadPlanner,
            SpreadRenderer spreadRenderer
    ) {
        this(view, sourceFactory, tasks, uiExecutor, cache, spreadPlanner, spreadRenderer, ReaderStore.disabled());
    }

    public AsyncViewerSession(ViewerView view, Function<File, ImageIterator> sourceFactory,
                              ViewerTaskExecutor tasks, Executor uiExecutor, BoundedImageCache cache,
                              SpreadPlanner spreadPlanner, SpreadRenderer spreadRenderer, ReaderStore store) {
        this.view = view;
        this.store = store;
        this.sourceFactory = sourceFactory;
        this.tasks = tasks;
        this.uiExecutor = uiExecutor;
        this.cache = cache;
        this.spreadPlanner = spreadPlanner;
        this.spreadRenderer = spreadRenderer;
    }

    private static int restoredIndex(BookState saved, List<PageRef> pages) {
        for (int index = 0; index < pages.size(); index++) {
            if (pages.get(index).key().pageId().equals(saved.pageId())) return index;
        }
        return Math.max(0, Math.min(saved.pageNumber() - 1, pages.size() - 1));
    }

    public void open(File file) {
        if (closed.get()) {
            return;
        }
        long targetGeneration = generation.incrementAndGet();
        long requestId = nextRequest();
        postLoading(targetGeneration, requestId);
        tasks.execute(INTERACTIVE, () -> openOnWorker(file, targetGeneration, requestId));
    }

    public void resetAndClose(Runnable afterReset, java.util.function.Consumer<String> onFailure) {
        if (!closed.compareAndSet(false, true)) return;
        // Invalidate already posted frames and prevent new saves while the reset is queued.
        generation.incrementAndGet();
        latestRequest.set(requestSequence.incrementAndGet());
        boolean queued = tasks.execute(INTERACTIVE, () -> {
            try {
                store.reset();
            } catch (RuntimeException exception) {
                closed.set(false);
                uiExecutor.execute(() -> onFailure.accept("Reset failed. The reader database could not be cleared. "
                        + "Your books were not touched. Check that storage is available and try again."));
                return;
            }
            tasks.close();
            try {
                closeOnWorker();
            } finally {
                uiExecutor.execute(afterReset);
            }
        });
        if (!queued) {
            closed.set(false);
            uiExecutor.execute(() -> onFailure.accept("Unable to start the reset. Please restart the application and try again."));
        }
    }

    public void remember(ViewerFrame frame) {
        if (closed.get() || frame.pageKey() == null) return;
        BookState state = new BookState(frame.pageKey().sourceId(), frame.pageKey().pageId(),
                frame.firstPage(), frame.pageCount(), frame.twoPageMode(), frame.readingMode());
        tasks.execute(INTERACTIVE, () -> {
            persist(() -> store.saveBook(state));
            if (!state.sourceId().equals(lastRememberedSource)) {
                lastRememberedSource = state.sourceId();
                refreshRecentBooks();
            }
        });
    }

    public void setGroupRecentByFolder(boolean grouped) {
        groupRecentByFolder = grouped;
        refreshRecentBooks();
    }

    public void hideRecentBook(String sourceId) {
        if (closed.get()) return;
        var ids = shownRecent.stream().filter(book -> book.state().sourceId().equals(sourceId))
                .findFirst().map(book -> book.chapters().stream().map(BookState::sourceId).toList()).orElse(List.of(sourceId));
        recentRequest.incrementAndGet();
        tasks.execute(INTERACTIVE, () -> {
            if (closed.get()) return;
            persist(() -> ids.forEach(store::hideRecent));
            refreshRecentBooks();
        });
    }

    public void refreshRecentBooks() {
        if (closed.get()) return;
        long request = recentRequest.incrementAndGet();
        tasks.execute(INTERACTIVE, () -> {
            if (!recentActive(request)) return;
            List<RecentBook> books;
            try {
                books = RecentGroups.select(store.recentBooks(1000), groupRecentByFolder, 12).stream().map(group -> {
                    var book = RecentBookLoader.inspect(group.get(0));
                    return new RecentBook(book.state(), book.available(), null, group);
                }).toList();
            } catch (RuntimeException exception) {
                warnPersistence();
                return;
            }
            var snapshots = new ArrayList<>(books);
            postRecent(request, snapshots);
            // One cover per low-priority task: page navigation can run between covers.
            for (int index = 0; index < books.size(); index++) {
                int position = index;
                tasks.execute(TaskPriority.THUMBNAIL, () -> {
                    if (!recentActive(request)) return;
                    snapshots.set(position, RecentBookLoader.cover(books.get(position)));
                    postRecent(request, snapshots);
                });
            }
        });
    }

    private boolean recentActive(long request) {
        return !closed.get() && recentRequest.get() == request;
    }

    private void postRecent(long request, List<RecentBook> books) {
        List<RecentBook> snapshot = List.copyOf(books);
        uiExecutor.execute(() -> {
            if (recentActive(request)) { shownRecent = snapshot; view.showRecentBooks(snapshot); }
        });
    }

    public void saveSettings(ReaderSettings settings) {
        if (!closed.get()) tasks.execute(INTERACTIVE, () -> persist(() -> store.saveSettings(settings)));
    }

    private void persist(Runnable write) {
        try {
            write.run();
        } catch (RuntimeException exception) {
            warnPersistence();
        }
    }

    private Optional<BookState> savedBook(String sourceId) {
        try {
            return store.book(sourceId);
        } catch (RuntimeException exception) {
            warnPersistence();
            return Optional.empty();
        }
    }

    private void warnPersistence() {
        if (persistenceWarningShown) return;
        persistenceWarningShown = true;
        uiExecutor.execute(() -> {
            if (!closed.get())
                view.showPersistenceWarning("Reading can continue, but settings or progress could not be saved. Check the reader database location and available disk space.");
        });
    }

    /**
     * Uses the source only on its owning worker, below reading and prefetch priority.
     */
    public void thumbnail(int index, PageRef expected, java.util.function.Consumer<BufferedImage> result) {
        long targetGeneration = generation.get();
        boolean queued = !closed.get() && tasks.execute(TaskPriority.THUMBNAIL, () -> {
            BufferedImage thumbnail = null;
            if (isActive(targetGeneration) && index >= 0 && index < pages.size()
                    && pages.get(index).equals(expected)) {
                try {
                    BufferedImage image = cache.get(expected.key());
                    if (image == null) image = decodeImage(source, expected);
                    thumbnail = org.dustyroom.ui.rendering.ThumbnailRenderer.render(image);
                } catch (RuntimeException ignored) { /* An unreadable preview must not interrupt reading. */ }
            }
            BufferedImage completed = thumbnail;
            uiExecutor.execute(() -> result.accept(isActive(targetGeneration) ? completed : null));
        });
        if (!queued) uiExecutor.execute(() -> result.accept(null));
    }

    public void next() {
        navigate(this::moveNext);
    }

    public void previous() {
        navigate(this::movePrevious);
    }

    public void first() {
        navigate(() -> pageIndex = 0);
    }

    public void last() {
        navigate(() -> pageIndex = pages.size() - 1);
    }

    public void goToPage(int pageNumber) {
        navigate(() -> {
            if (pageNumber < 1 || pageNumber > pages.size()) {
                throw new IllegalArgumentException("Page must be between 1 and " + pages.size());
            }
            pageIndex = pageNumber - 1;
        });
    }

    public void nextVolume() {
        switchVolume(true);
    }

    public void previousVolume() {
        switchVolume(false);
    }

    public void toggleTwoPageMode() {
        updatePresentation(() -> twoPageMode = !twoPageMode);
    }

    public void setComicsReadingMode() {
        updatePresentation(() -> readingMode = COMICS);
    }

    public void setMangaReadingMode() {
        updatePresentation(() -> readingMode = MANGA);
    }

    @Override
    public void close() {
        close(null);
    }

    public void close(Runnable afterClose) {
        boolean initiateClose;
        boolean scheduleCallbackDrain = false;
        synchronized (closeMonitor) {
            if (afterClose != null) {
                closeCallbacks.add(afterClose);
                if (cleanupComplete && !closeCallbackDrainScheduled) {
                    closeCallbackDrainScheduled = true;
                    scheduleCallbackDrain = true;
                }
            }

            initiateClose = closed.compareAndSet(false, true);
        }

        if (scheduleCallbackDrain) {
            uiExecutor.execute(this::drainCloseCallbacks);
        }
        if (!initiateClose) {
            return;
        }

        generation.incrementAndGet();
        latestRequest.set(requestSequence.incrementAndGet());
        boolean cleanupQueued = tasks.execute(INTERACTIVE, this::closeOnWorker);
        tasks.close();
        if (!cleanupQueued) {
            closeOnWorker();
        }
    }

    private void closeOnWorker() {
        try {
            closeSource(source);
        } finally {
            source = null;
            pages = List.of();
            shapes.clear();
            cache.clear();
            pageIndex = -1;
            lastSourceError = null;
            try {
                store.close();
            } finally {
                completeClose();
            }
        }
    }

    private void completeClose() {
        boolean scheduleCallbackDrain = false;
        synchronized (closeMonitor) {
            if (cleanupComplete) {
                return;
            }
            cleanupComplete = true;
            if (!closeCallbacks.isEmpty() && !closeCallbackDrainScheduled) {
                closeCallbackDrainScheduled = true;
                scheduleCallbackDrain = true;
            }
        }
        if (scheduleCallbackDrain) {
            uiExecutor.execute(this::drainCloseCallbacks);
        }
    }

    private void drainCloseCallbacks() {
        while (true) {
            Runnable callback;
            synchronized (closeMonitor) {
                if (closeCallbacks.isEmpty()) {
                    closeCallbackDrainScheduled = false;
                    return;
                }
                callback = closeCallbacks.remove(0);
            }
            try {
                callback.run();
            } catch (RuntimeException ignored) {
                // One shutdown callback must not prevent the remaining callbacks.
            }
        }
    }

    private void openOnWorker(File file, long targetGeneration, long requestId) {
        if (!isActive(targetGeneration)) {
            return;
        }

        ImageIterator candidate = null;
        try {
            candidate = sourceFactory.apply(file);
            if (!isActive(targetGeneration)) {
                closeCandidate(candidate);
                return;
            }

            List<PageRef> candidatePages = List.copyOf(candidate.pages());
            if (candidatePages.isEmpty()) {
                throw new IllegalStateException("Image source contains no pages");
            }
            int candidateIndex = candidate.initialPageIndex();
            if (candidateIndex < 0 || candidateIndex >= candidatePages.size()) {
                throw new IllegalStateException("Image source has an invalid initial page");
            }
            Optional<BookState> saved = savedBook(candidatePages.get(0).key().sourceId());
            // An explicitly selected image takes precedence over the saved position of its folder.
            if (saved.isPresent() && !org.dustyroom.be.utils.FileUtils.isImage.test(file)) {
                candidateIndex = restoredIndex(saved.get(), candidatePages);
            }
            PageRef initialPage = candidatePages.get(candidateIndex);
            BufferedImage initialImage = decodeImage(candidate, initialPage);
            if (!isActive(targetGeneration)) {
                closeCandidate(candidate);
                return;
            }

            ImageIterator previousSource = source;
            source = candidate;
            candidate = null;
            pages = candidatePages;
            pageIndex = candidateIndex;
            saved.ifPresent(state -> {
                twoPageMode = state.twoPageMode();
                readingMode = state.readingMode();
            });
            shapes.clear();
            cache.clear();
            shapes.put(initialPage.key(), PageShape.from(initialImage));
            cache.put(initialPage.key(), initialImage);
            lastSourceError = null;
            if (previousSource != source) {
                closeSource(previousSource);
            }

            persist(() -> store.showRecent(initialPage.key().sourceId()));
            lastRememberedSource = null;
            renderAndPublish(targetGeneration, requestId);
        } catch (RuntimeException e) {
            closeCandidate(candidate);
            lastSourceError = errorMessage(e);
            postError(targetGeneration, requestId, e);
        }
    }

    private void navigate(Runnable navigation) {
        submitInteractive((targetGeneration, requestId) -> {
            int previousIndex = pageIndex;
            try {
                navigation.run();
                renderAndPublish(targetGeneration, requestId);
            } catch (RuntimeException e) {
                pageIndex = previousIndex;
                throw e;
            }
        });
    }

    private void switchVolume(boolean next) {
        submitInteractive((targetGeneration, requestId) -> {
            PageRef selected = next
                    ? source.switchToNextVolume()
                    : source.switchToPreviousVolume();
            List<PageRef> switchedPages = List.copyOf(source.pages());
            if (selected == null || switchedPages.isEmpty()) {
                throw new IllegalStateException("The selected volume contains no pages");
            }

            int selectedIndex = switchedPages.indexOf(selected);
            pages = switchedPages;
            pageIndex = selectedIndex < 0 ? source.initialPageIndex() : selectedIndex;
            savedBook(pages.get(0).key().sourceId()).ifPresent(state -> {
                pageIndex = restoredIndex(state, pages);
                twoPageMode = state.twoPageMode();
                readingMode = state.readingMode();
            });
            shapes.clear();
            cache.clear();
            persist(() -> store.showRecent(selected.key().sourceId()));
            lastRememberedSource = null;
            renderAndPublish(targetGeneration, requestId);
        });
    }

    private void updatePresentation(Runnable update) {
        submitInteractive((targetGeneration, requestId) -> {
            update.run();
            renderAndPublish(targetGeneration, requestId);
        });
    }

    private void submitInteractive(WorkerCommand command) {
        if (closed.get()) {
            return;
        }
        long targetGeneration = generation.get();
        long requestId = nextRequest();
        tasks.execute(INTERACTIVE, () -> {
            if (!isActive(targetGeneration)) {
                return;
            }
            if (source == null || pages.isEmpty()) {
                if (lastSourceError != null) {
                    postError(
                            targetGeneration,
                            requestId,
                            new IllegalStateException(lastSourceError)
                    );
                }
                return;
            }
            postLoading(targetGeneration, requestId);
            try {
                command.run(targetGeneration, requestId);
            } catch (RuntimeException e) {
                postError(targetGeneration, requestId, e);
            }
        });
    }

    private void moveNext() {
        PageRef secondPage = resolveSecondPage(pageIndex);
        int step = secondPage == null ? 1 : 2;
        pageIndex = Math.floorMod(pageIndex + step, pages.size());
    }

    private void movePrevious() {
        int pageCount = pages.size();
        for (int distance = 1; distance <= Math.min(2, pageCount); distance++) {
            int candidate = Math.floorMod(pageIndex - distance, pageCount);
            int step = resolveSecondPage(candidate) == null ? 1 : 2;
            if (Math.floorMod(candidate + step, pageCount) == pageIndex) {
                pageIndex = candidate;
                return;
            }
        }
        pageIndex = Math.floorMod(pageIndex - 1, pageCount);
    }

    private void renderAndPublish(long targetGeneration, long requestId) {
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }
        PageRef current = pages.get(pageIndex);
        BufferedImage currentImage = loadImage(current);
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }
        PageRef second = resolveSecondPage(pageIndex);
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }
        BufferedImage secondImage = second == null ? null : loadImage(second);
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }

        BufferedImage composed = spreadRenderer.compose(
                currentImage,
                secondImage,
                readingMode == MANGA
        );
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }
        ViewerFrame frame = new ViewerFrame(
                composed,
                spreadRenderer.buildTitle(current.metadata(), second),
                current.metadata().dir(),
                pageIndex + 1, pageIndex + (second == null ? 1 : 2), pages.size(),
                twoPageMode, readingMode, current.key()
        );
        postFrame(targetGeneration, requestId, frame);
        scheduleAnimations(targetGeneration, requestId, current, second, currentImage, secondImage, readingMode == MANGA);
        schedulePrefetch(targetGeneration, requestId, pageIndex, second != null);
    }

    private void scheduleAnimations(long targetGeneration, long requestId, PageRef first, PageRef second,
                                    BufferedImage firstImage, BufferedImage secondImage, boolean manga) {
        var refs = second == null ? List.of(first) : List.of(first, second);
        if (refs.stream().noneMatch(page -> page.key().pageId().toLowerCase(java.util.Locale.ROOT).endsWith(".gif"))) return;
        tasks.execute(PREFETCH, () -> {
            if (!isLatest(targetGeneration, requestId)) return;
            var animated = new ArrayList<AnimatedPage>();
            for (int index = 0; index < refs.size(); index++) {
                if (!isLatest(targetGeneration, requestId)) return;
                PageRef page = refs.get(index);
                if (!page.key().pageId().toLowerCase(java.util.Locale.ROOT).endsWith(".gif")) continue;
                try {
                    var clip = source.animation(page);
                    if (clip == null) continue;
                    BufferedImage image = index == 0 ? firstImage : secondImage;
                    int x = second == null ? 0 : index == 0 ? (manga ? secondImage.getWidth() : 0) : (manga ? 0 : firstImage.getWidth());
                    int height = second == null ? firstImage.getHeight() : Math.max(firstImage.getHeight(), secondImage.getHeight());
                    animated.add(new AnimatedPage(clip, x, (height - image.getHeight()) / 2, image.getWidth(), image.getHeight(), second != null));
                } catch (RuntimeException exception) {
                    postError(targetGeneration, requestId, exception);
                }
            }
            uiExecutor.execute(() -> {
                if (isLatest(targetGeneration, requestId)) view.showAnimations(first.key(), List.copyOf(animated));
            });
        });
    }

    private PageRef resolveSecondPage(int anchorIndex) {
        if (!twoPageMode || pages.size() < 2 || anchorIndex + 1 >= pages.size()) {
            return null;
        }
        PageRef current = pages.get(anchorIndex);
        PageRef next = pages.get(anchorIndex + 1);
        PageShape currentShape = shapeOf(current);
        PageShape nextShape = shapeOf(next);
        return spreadPlanner.shouldPair(currentShape, nextShape, true) ? next : null;
    }

    private PageShape shapeOf(PageRef page) {
        PageShape shape = shapes.get(page.key());
        if (shape != null) {
            return shape;
        }
        return PageShape.from(loadImage(page));
    }

    private BufferedImage loadImage(PageRef page) {
        BufferedImage cached = cache.get(page.key());
        if (cached != null) {
            shapes.putIfAbsent(page.key(), PageShape.from(cached));
            return cached;
        }

        BufferedImage image = decodeImage(source, page);
        shapes.put(page.key(), PageShape.from(image));
        cache.put(page.key(), image);
        return image;
    }

    private BufferedImage decodeImage(ImageIterator imageSource, PageRef page) {
        Picture loaded = imageSource.load(page);
        if (loaded == null || loaded.image() == null) {
            throw new IllegalStateException("Image source returned an empty page");
        }
        return loaded.image();
    }

    private void schedulePrefetch(
            long targetGeneration,
            long requestId,
            int anchorIndex,
            boolean spreadVisible
    ) {
        if (!isLatest(targetGeneration, requestId)) {
            return;
        }
        Set<PageRef> targets = new LinkedHashSet<>();
        int[] offsets = spreadVisible ? new int[]{2, 3} : new int[]{1};
        for (int offset : offsets) {
            if (pages.size() <= 1) {
                break;
            }
            PageRef candidate = pages.get(Math.floorMod(anchorIndex + offset, pages.size()));
            if (!candidate.equals(pages.get(anchorIndex))) {
                targets.add(candidate);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        List<PageRef> capturedTargets = new ArrayList<>(targets);
        tasks.execute(PREFETCH, () -> {
            if (!isLatest(targetGeneration, requestId)) {
                return;
            }
            for (PageRef target : capturedTargets) {
                if (!isLatest(targetGeneration, requestId)) {
                    return;
                }
                try {
                    loadImage(target);
                } catch (RuntimeException ignored) {
                    // Prefetch is opportunistic; interactive loading reports errors.
                }
            }
        });
    }

    private void postLoading(long targetGeneration, long requestId) {
        uiExecutor.execute(() -> {
            if (isLatest(targetGeneration, requestId)) {
                view.showLoading();
            }
        });
    }

    private void postFrame(long targetGeneration, long requestId, ViewerFrame frame) {
        List<PageRef> capturedPages = pages;
        uiExecutor.execute(() -> {
            if (isLatest(targetGeneration, requestId)) {
                view.showPages(capturedPages);
                view.showFrame(frame);
            }
        });
    }

    private void postError(long targetGeneration, long requestId, RuntimeException error) {
        String finalMessage = errorMessage(error);
        uiExecutor.execute(() -> {
            if (isLatest(targetGeneration, requestId)) {
                view.showError(finalMessage);
            }
        });
    }

    private String errorMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? "Unable to load the selected image"
                : message;
    }

    private long nextRequest() {
        long requestId = requestSequence.incrementAndGet();
        latestRequest.set(requestId);
        return requestId;
    }

    private boolean isActive(long targetGeneration) {
        return !closed.get() && generation.get() == targetGeneration;
    }

    private boolean isLatest(long targetGeneration, long requestId) {
        return isActive(targetGeneration) && latestRequest.get() == requestId;
    }

    private void closeSource(ImageIterator iterator) {
        if (iterator != null) {
            iterator.close();
        }
    }

    private void closeCandidate(ImageIterator candidate) {
        if (candidate != source) {
            closeSource(candidate);
        }
    }

    @FunctionalInterface
    private interface WorkerCommand {
        void run(long targetGeneration, long requestId);
    }
}
