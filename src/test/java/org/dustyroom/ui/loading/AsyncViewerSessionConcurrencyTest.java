package org.dustyroom.ui.loading;

import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.models.PageKey;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.rendering.SpreadRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncViewerSessionConcurrencyTest {
    @Test
    void closeWaitsForInFlightDecodeAndSuppressesItsCallback() throws Exception {
        BlockingIterator source = new BlockingIterator();
        RecordingView view = new RecordingView();
        CountDownLatch sessionClosed = new CountDownLatch(1);
        AsyncViewerSession session = session(source, view);

        session.open(new File("source"));
        assertTrue(source.loadStarted.await(2, TimeUnit.SECONDS));

        session.close(sessionClosed::countDown);
        assertEquals(0, source.closeCount.get());

        source.releaseLoad.countDown();
        assertTrue(sessionClosed.await(2, TimeUnit.SECONDS));
        assertEquals(1, source.closeCount.get());
        assertEquals(0, view.frames.get());
        assertEquals(0, view.errors.get());
    }

    private static AsyncViewerSession session(BlockingIterator source, RecordingView view) {
        return new AsyncViewerSession(
                view,
                file -> source,
                new SerialPriorityExecutor(),
                Runnable::run,
                new BoundedImageCache(4, 1024 * 1024),
                new SpreadPlanner(),
                new SpreadRenderer()
        );
    }

    private static final class RecordingView implements ViewerView {
        private final AtomicInteger frames = new AtomicInteger();
        private final AtomicInteger errors = new AtomicInteger();

        @Override
        public void showLoading() {
        }

        @Override
        public void showFrame(ViewerFrame frame) {
            frames.incrementAndGet();
        }

        @Override
        public void showError(String message) {
            errors.incrementAndGet();
        }
    }

    private static final class BlockingIterator implements ImageIterator {
        private final PageRef page = new PageRef(
                new PageKey("source", "page"),
                new PictureMetadata("page", "source", new File("source"))
        );
        private final CountDownLatch loadStarted = new CountDownLatch(1);
        private final CountDownLatch releaseLoad = new CountDownLatch(1);
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void init() {
        }

        @Override
        public File getVolumeRoot() {
            return new File("source");
        }

        @Override
        public void setFile(File file) {
        }

        @Override
        public Picture next() {
            return load(page);
        }

        @Override
        public Picture prev() {
            return load(page);
        }

        @Override
        public Picture first() {
            return load(page);
        }

        @Override
        public Picture last() {
            return load(page);
        }

        @Override
        public Picture nextVol() {
            return load(page);
        }

        @Override
        public Picture prevVol() {
            return load(page);
        }

        @Override
        public List<PageRef> pages() {
            return List.of(page);
        }

        @Override
        public int initialPageIndex() {
            return 0;
        }

        @Override
        public Picture load(PageRef page) {
            loadStarted.countDown();
            await(releaseLoad);
            return new Picture(new BufferedImage(4, 8, BufferedImage.TYPE_INT_ARGB), page.metadata());
        }

        @Override
        public PageRef switchToNextVolume() {
            return page;
        }

        @Override
        public PageRef switchToPreviousVolume() {
            return page;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        private void await(CountDownLatch latch) {
            try {
                if (!latch.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for test latch");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
