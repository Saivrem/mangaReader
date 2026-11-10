package org.dustyroom.ui.loading;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RecentBookLoaderTest {
    @TempDir
    Path directory;

    private BookState state(Path path) {
        return new BookState(path.toString(), "page", 2, 10, false, ReadingMode.MANGA);
    }

    @Test
    void missingSourceRetainsProgressAndCanBecomeAvailableAgain() throws Exception {
        Path path = directory.resolve("moved");
        RecentBook missing = RecentBookLoader.inspect(state(path));
        assertFalse(missing.isAvailable());
        assertEquals("Page 2 / 10", missing.getProgress());
        Files.createDirectory(path);
        assertTrue(RecentBookLoader.inspect(state(path)).isAvailable());
    }

    @Test
    void archiveCoverIsFirstPageAndHandleIsClosed() throws Exception {
        Path archive = directory.resolve("日本語 book.zip");
        BufferedImage image = new BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff123456);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("01.png"));
            ImageIO.write(image, "png", zip);
            zip.closeEntry();
        }
        RecentBook book = RecentBookLoader.cover(RecentBookLoader.inspect(state(archive)));
        assertTrue(book.isAvailable());
        assertNotNull(book.getCover());
        assertEquals(0xff123456, book.getCover().getRGB(0, 0));
        Files.delete(archive); // Windows also proves that ZipFile was closed.
        assertFalse(RecentBookLoader.inspect(book.getState()).isAvailable());
    }

    @Test
    void brokenCoverDoesNotDestroyHistory() throws Exception {
        Path archive = Files.writeString(directory.resolve("broken.zip"), "invalid zip");
        RecentBook book = RecentBookLoader.cover(RecentBookLoader.inspect(state(archive)));
        assertEquals(state(archive), book.getState());
        assertTrue(book.isAvailable());
        assertNull(book.getCover());
    }
}
