package org.dustyroom.persistence;

import org.dustyroom.be.iterators.ImageIteratorFactory;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.ui.loading.*;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.rendering.SpreadRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.dustyroom.be.iterators.ImageTestFixtures.writeImage;
import static org.dustyroom.be.iterators.ImageTestFixtures.writeZip;
import static org.junit.jupiter.api.Assertions.*;

class ReadingPersistenceTest extends BasePersistenceTest {

    private AsyncViewerSession session(AtomicReference<ViewerFrame> shown) {
        ViewerView view = new ViewerView() {
            public void showLoading() {
            }

            public void showFrame(ViewerFrame frame) {
                shown.set(frame);
            }

            public void showError(String error) {
                fail(error);
            }

            public void showPersistenceWarning(String error) {
                fail(error);
            }
        };
        ViewerTaskExecutor direct = new ViewerTaskExecutor() {
            public boolean execute(TaskPriority priority, Runnable task) {
                task.run();
                return true;
            }

            public void close() {
            }
        };
        return new AsyncViewerSession(view, ImageIteratorFactory::create, direct, Runnable::run,
                new BoundedImageCache(12, 1024 * 1024), new SpreadPlanner(), new SpreadRenderer(),
                openStore());
    }

    private Path createBook(String name) throws IOException {
        Path source = directory.resolve(name);
        if (name.endsWith(".zip")) {
            return writeZip(source, "chapter/01.png", "chapter/02.png", "chapter/03.png");
        }

        for (String page : new String[]{"01.png", "02.png", "03.png"}) {
            writeImage(source, page);
        }
        return source;
    }

    @ParameterizedTest
    @ValueSource(strings = {"pages", "book.zip"})
    void resumesReadingAfterReopen(String name) throws IOException {
        Path source = createBook(name);
        AtomicReference<ViewerFrame> shown = new AtomicReference<>();
        try (AsyncViewerSession first = session(shown)) {
            first.open(source.toFile());
            first.goToPage(2);
            first.toggleTwoPageMode();
            first.setComicsReadingMode();
            first.remember(shown.get());
        }

        shown.set(null);
        try (AsyncViewerSession reopened = session(shown)) {
            reopened.open(source.toFile());
            ViewerFrame frame = shown.get();
            assertEquals(2, frame.getFirstPage());
            assertEquals(3, frame.getLastPage());
            assertTrue(frame.isTwoPageMode());
            assertEquals(ReadingMode.COMICS, frame.getReadingMode());
        }
    }

    @Test
    void newBooksUseDefaultsButSavedBooksKeepTheirModes() throws IOException {
        Path firstFolder = createBook("first");
        Path secondFolder = createBook("second");
        AtomicReference<ViewerFrame> shown = new AtomicReference<>();
        try (AsyncViewerSession reader = session(shown)) {
            reader.configureDefaults(ReaderSettings.defaults().setDefaultReadingMode(ReadingMode.COMICS).setDefaultTwoPageMode(true));
            reader.open(firstFolder.toFile());
            assertEquals(ReadingMode.COMICS, shown.get().getReadingMode());
            assertTrue(shown.get().isTwoPageMode());
            reader.remember(shown.get());
            reader.configureDefaults(ReaderSettings.defaults());
            reader.open(secondFolder.toFile());
            assertEquals(ReadingMode.MANGA, shown.get().getReadingMode());
            assertFalse(shown.get().isTwoPageMode());
            reader.open(firstFolder.toFile());
            assertEquals(ReadingMode.COMICS, shown.get().getReadingMode());
            assertTrue(shown.get().isTwoPageMode());
        }
    }
}
