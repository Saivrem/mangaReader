package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Picture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.dustyroom.be.iterators.ImageTestFixtures.jpegWithConflictingColorMarkers;
import static org.dustyroom.be.iterators.ImageTestFixtures.writeImage;

class FileImageIteratorTest {
    @TempDir
    Path tempDir;

    @Test
    void startsAtSelectedFileAndNavigatesInNaturalCircularOrder() throws Exception {
        Path page1 = writeImage(tempDir, "page1.png");
        Path page2 = writeImage(tempDir, "page2.PNG");
        Path page10 = writeImage(tempDir, "page10.png");
        Files.createFile(tempDir.resolve("volume.zip"));
        assertTrue(Files.exists(page1));
        assertTrue(Files.exists(page10));

        FileImageIterator iterator = new FileImageIterator(page2.toFile());

        assertName("page2.PNG", iterator.next());
        assertName("page10.png", iterator.next());
        assertName("page1.png", iterator.next());
        assertName("page10.png", iterator.prev());
        assertName("page1.png", iterator.first());
        assertName("page2.PNG", iterator.next());
        assertName("page10.png", iterator.last());
        assertName("page1.png", iterator.next());
    }

    @Test
    void initialPreviousReturnsThePredecessorAndWraps() throws Exception {
        Path page1 = writeImage(tempDir, "page1.png");
        Path page2 = writeImage(tempDir, "page2.png");

        assertName("page1.png", new FileImageIterator(page2.toFile()).prev());
        assertName("page2.png", new FileImageIterator(page1.toFile()).prev());
    }

    @Test
    void exposesStableMetadata() throws Exception {
        Path page = writeImage(tempDir, "page.png");

        Picture picture = new FileImageIterator(page.toFile()).next();

        assertEquals("page.png", picture.metadata().name());
        assertEquals(tempDir.getFileName().toString(), picture.metadata().fileName());
        assertEquals(tempDir.toFile(), picture.metadata().dir());
        assertNotNull(picture.image());
    }

    @Test
    void rejectsEmptySourcesAndCorruptImagesWithoutReturningNullImages() throws Exception {
        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        Path broken = Files.writeString(tempDir.resolve("broken.JPG"), "not an image");

        assertThrows(ImageIteratorException.class, () -> new FileImageIterator(empty.toFile()));

        FileImageIterator iterator = new FileImageIterator(broken.toFile());
        ImageIteratorException error = assertThrows(ImageIteratorException.class, iterator::next);
        assertEquals(broken.toFile(), error.getSource());
    }

    @Test
    void decodesJfifWithConflictingAdobeColorMarker() throws Exception {
        Path page = Files.write(tempDir.resolve("page.jpg"), jpegWithConflictingColorMarkers());

        Picture picture = new FileImageIterator(page.toFile()).next();

        assertBlue(picture);
    }

    @Test
    void switchesBetweenNaturallySortedImageVolumesAndSkipsEmptyDirectories() throws Exception {
        Path volume1 = Files.createDirectory(tempDir.resolve("volume1"));
        Path volume2 = Files.createDirectory(tempDir.resolve("volume2"));
        Path volume10 = Files.createDirectory(tempDir.resolve("volume10"));
        Files.createDirectory(tempDir.resolve("volume3-empty"));
        Path page1 = writeImage(volume1, "page.png");
        Path page2 = writeImage(volume2, "page.png");
        writeImage(volume10, "page.png");

        FileImageIterator iterator = new FileImageIterator(page2.toFile());
        assertName("page.png", iterator.next());
        assertEquals(volume2.toFile(), iterator.getVolumeRoot());

        assertEquals("volume10", iterator.nextVol().metadata().fileName());
        assertEquals("volume1", iterator.nextVol().metadata().fileName());
        assertEquals("volume10", iterator.prevVol().metadata().fileName());

        assertTrue(Files.exists(page1));
    }

    @Test
    void legacyVolumeNavigationConsumesTheReturnedFirstPage() throws Exception {
        Path volume1 = Files.createDirectory(tempDir.resolve("volume1"));
        Path volume2 = Files.createDirectory(tempDir.resolve("volume2"));
        Path volume1Page1 = writeImage(volume1, "page1.png");
        writeImage(volume1, "page2.png");
        writeImage(volume2, "page1.png");
        writeImage(volume2, "page2.png");

        FileImageIterator iterator = new FileImageIterator(volume1Page1.toFile());
        assertName("page1.png", iterator.next());

        Picture nextVolume = iterator.nextVol();
        assertEquals("volume2", nextVolume.metadata().fileName());
        assertName("page1.png", nextVolume);
        assertName("page2.png", iterator.next());

        Picture previousVolume = iterator.prevVol();
        assertEquals("volume1", previousVolume.metadata().fileName());
        assertName("page1.png", previousVolume);
        assertName("page2.png", iterator.next());
    }

    @Test
    void failedReinitializationLeavesTheCurrentVolumeUsable() throws Exception {
        Path volume = Files.createDirectory(tempDir.resolve("volume"));
        Path page1 = writeImage(volume, "page1.png");
        writeImage(volume, "page2.png");
        Path empty = Files.createDirectory(tempDir.resolve("empty"));

        FileImageIterator iterator = new FileImageIterator(page1.toFile());
        assertName("page1.png", iterator.next());

        iterator.setFile(empty.toFile());
        assertThrows(ImageIteratorException.class, iterator::init);

        assertEquals(volume.toAbsolutePath().normalize().toFile(), iterator.getVolumeRoot());
        assertName("page2.png", iterator.next());
    }

    @Test
    void normalizesRelativeSourcesToStableAbsolutePaths() throws Exception {
        Path page = writeImage(tempDir, "page.png").toAbsolutePath().normalize();
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path relativePage = workingDirectory.relativize(page);

        FileImageIterator iterator = new FileImageIterator(relativePage.toFile());

        assertTrue(iterator.getVolumeRoot().isAbsolute());
        assertEquals(page.getParent(), iterator.getVolumeRoot().toPath());
        assertName("page.png", iterator.next());
    }

    @Test
    void reportsWhenTheCurrentVolumeNoLongerContainsImages() throws Exception {
        Path volume = Files.createDirectory(tempDir.resolve("volume"));
        Path page = writeImage(volume, "page.png");
        FileImageIterator iterator = new FileImageIterator(page.toFile());
        assertName("page.png", iterator.next());

        Files.delete(page);

        ImageIteratorException error = assertThrows(ImageIteratorException.class, iterator::nextVol);
        assertEquals(volume.toAbsolutePath().normalize().toFile(), error.getSource());
    }

    private static void assertName(String expected, Picture picture) {
        assertNotNull(picture);
        assertEquals(expected, picture.metadata().name());
    }

    private static void assertBlue(Picture picture) {
        int rgb = picture.image().getRGB(1, 1);
        assertTrue((rgb >>> 16 & 0xff) < 20);
        assertTrue((rgb >>> 8 & 0xff) < 20);
        assertTrue((rgb & 0xff) > 200);
    }
}
