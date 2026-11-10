package org.dustyroom.persistence;

import org.dustyroom.be.iterators.ImageIteratorFactory;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.dustyroom.ui.loading.*;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.rendering.SpreadRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ReadingPersistenceTest {
    @TempDir
    Path directory;

    private static AsyncViewerSession session(Path database, AtomicReference<ViewerFrame> shown) {
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
                new SqliteReaderStore(database));
    }

    @Test
    void resumesRealFolderAndZipWithReopenedDatabase() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("pages"));
        Path archive = directory.resolve("book.zip");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String page : new String[]{"01.png", "02.png", "03.png"}) {
                Files.write(folder.resolve(page), bytes.toByteArray());
                zip.putNextEntry(new ZipEntry("chapter/" + page));
                zip.write(bytes.toByteArray());
                zip.closeEntry();
            }
        }
        Path database = directory.resolve("reader.sqlite3");
        for (Path source : new Path[]{folder, archive}) {
            AtomicReference<ViewerFrame> shown = new AtomicReference<>();
            AsyncViewerSession first = session(database, shown);
            first.open(source.toFile());
            first.goToPage(2);
            first.toggleTwoPageMode();
            first.setComicsReadingMode();
            first.remember(shown.get());
            first.close();
            AsyncViewerSession reopened = session(database, shown);
            reopened.open(source.toFile());
            assertEquals(2, shown.get().getFirstPage());
            assertEquals(3, shown.get().getLastPage());
            assertTrue(shown.get().isTwoPageMode());
            assertEquals(ReadingMode.COMICS, shown.get().getReadingMode());
            reopened.close();
        }
    }

    @Test
    void newBooksUseDefaultsButSavedBooksKeepTheirModes() throws Exception {
        Path firstFolder = Files.createDirectory(directory.resolve("first"));
        Path secondFolder = Files.createDirectory(directory.resolve("second"));
        for (Path folder : new Path[]{firstFolder, secondFolder}) {
            for (String name : new String[]{"01.png", "02.png", "03.png"}) {
                ImageIO.write(new BufferedImage(4, 8, BufferedImage.TYPE_INT_RGB), "png", folder.resolve(name).toFile());
            }
        }
        AtomicReference<ViewerFrame> shown = new AtomicReference<>();
        AsyncViewerSession reader = session(directory.resolve("reader.sqlite3"), shown);
        try {
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
        } finally {
            reader.close();
        }
    }
}
