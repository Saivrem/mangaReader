package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Picture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.dustyroom.be.iterators.ImageTestFixtures.pngBytes;
import static org.dustyroom.be.iterators.ImageTestFixtures.writeZip;

class ZipIteratorTest {
    @TempDir
    Path tempDir;

    @Test
    void readsOnlyImagesInNaturalCircularOrder() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("page10.PNG", pngBytes());
        entries.put("page2.jpg", pngBytes());
        entries.put("page1.png", pngBytes());
        entries.put("nested.zip", new byte[]{1, 2, 3});
        entries.put("__MACOSX/junk.jpg", pngBytes());
        entries.put("notes.txt", new byte[]{1});
        Path zip = writeZip(tempDir.resolve("volume.ZIP"), entries);

        try (ZipIterator iterator = new ZipIterator(zip.toFile())) {
            assertName("page1.png", iterator.next());
            assertName("page2.jpg", iterator.next());
            assertName("page10.PNG", iterator.next());
            assertName("page1.png", iterator.next());
            assertName("page10.PNG", iterator.prev());
            assertName("page1.png", iterator.first());
            assertName("page10.PNG", iterator.last());
        }
    }

    @Test
    void initialPreviousReturnsTheLastImage() throws Exception {
        Path zip = writeZip(tempDir.resolve("volume.zip"), "page1.png", "page2.png");

        try (ZipIterator iterator = new ZipIterator(zip.toFile())) {
            assertName("page2.png", iterator.prev());
        }
    }

    @Test
    void keepsNestedEntryMetadata() throws Exception {
        Path zip = writeZip(tempDir.resolve("volume.zip"), "chapter2/page01.png");

        try (ZipIterator iterator = new ZipIterator(zip.toFile())) {
            Picture picture = iterator.next();
            assertEquals("chapter2/page01.png", picture.metadata().name());
            assertEquals("volume.zip", picture.metadata().fileName());
            assertEquals(tempDir.toFile(), picture.metadata().dir());
            assertNotNull(picture.image());
        }
    }

    @Test
    void rejectsEmptyCorruptArchivesAndCorruptImageEntries() throws Exception {
        Path empty = writeZip(tempDir.resolve("empty.zip"));
        Path corruptArchive = Files.writeString(tempDir.resolve("corrupt.zip"), "not a zip");
        Path corruptImage = writeZip(
                tempDir.resolve("bad-image.zip"),
                Map.of("page.jpg", "not an image".getBytes())
        );

        assertThrows(ImageIteratorException.class, () -> new ZipIterator(empty.toFile()));
        assertThrows(ImageIteratorException.class, () -> new ZipIterator(corruptArchive.toFile()));

        try (ZipIterator iterator = new ZipIterator(corruptImage.toFile())) {
            assertThrows(ImageIteratorException.class, iterator::next);
        }
    }

    @Test
    void closeIsIdempotentAndClosedIteratorCannotRead() throws Exception {
        Path zip = writeZip(tempDir.resolve("volume.zip"), "page.png");
        ZipIterator iterator = new ZipIterator(zip.toFile());

        iterator.close();
        iterator.close();

        assertThrows(ImageIteratorException.class, iterator::next);
        assertThrows(ImageIteratorException.class, iterator::init);
        assertThrows(ImageIteratorException.class, iterator::nextVol);
        assertThrows(ImageIteratorException.class, () -> iterator.setFile(zip.toFile()));
    }

    @Test
    void failedVolumeSwitchLeavesCurrentArchiveUsable() throws Exception {
        Path volume1 = writeZip(tempDir.resolve("volume1.zip"), "page.png");
        Files.writeString(tempDir.resolve("volume2.zip"), "broken");
        writeZip(tempDir.resolve("volume10.ZIP"), "other.png");

        try (ZipIterator iterator = new ZipIterator(volume1.toFile())) {
            assertEquals("volume1.zip", iterator.next().metadata().fileName());
            assertThrows(ImageIteratorException.class, iterator::nextVol);
            assertEquals("volume1.zip", iterator.next().metadata().fileName());
        }
    }

    @Test
    void switchesVolumesCaseInsensitivelyInNaturalOrder() throws Exception {
        Path volume1 = writeZip(tempDir.resolve("volume1.zip"), "one.png");
        writeZip(tempDir.resolve("volume2.ZIP"), "two.png");
        writeZip(tempDir.resolve("volume10.zip"), "ten.png");

        try (ZipIterator iterator = new ZipIterator(volume1.toFile())) {
            assertEquals("volume1.zip", iterator.next().metadata().fileName());
            assertEquals("volume2.ZIP", iterator.nextVol().metadata().fileName());
            assertEquals("volume10.zip", iterator.nextVol().metadata().fileName());
            assertEquals("volume1.zip", iterator.nextVol().metadata().fileName());
            assertEquals("volume10.zip", iterator.prevVol().metadata().fileName());
        }
    }

    @Test
    void legacyVolumeNavigationConsumesTheReturnedFirstPage() throws Exception {
        Path volume1 = writeZip(tempDir.resolve("volume1.zip"), "page1.png", "page2.png");
        writeZip(tempDir.resolve("volume2.zip"), "page1.png", "page2.png");

        try (ZipIterator iterator = new ZipIterator(volume1.toFile())) {
            assertName("page1.png", iterator.next());

            Picture nextVolume = iterator.nextVol();
            assertEquals("volume2.zip", nextVolume.metadata().fileName());
            assertName("page1.png", nextVolume);
            assertName("page2.png", iterator.next());

            Picture previousVolume = iterator.prevVol();
            assertEquals("volume1.zip", previousVolume.metadata().fileName());
            assertName("page1.png", previousVolume);
            assertName("page2.png", iterator.next());
        }
    }

    @Test
    void normalizesRelativeSourcesToStableAbsolutePaths() throws Exception {
        Path zip = writeZip(tempDir.resolve("volume.zip"), "page.png").toAbsolutePath().normalize();
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path relativeZip = workingDirectory.relativize(zip);

        try (ZipIterator iterator = new ZipIterator(relativeZip.toFile())) {
            assertTrue(iterator.getVolumeRoot().isAbsolute());
            assertEquals(zip, iterator.getVolumeRoot().toPath());
            assertEquals(zip.getParent().toFile(), iterator.next().metadata().dir());
        }
    }

    @Test
    void reportsWhenTheCurrentVolumeDisappears() throws Exception {
        Path volume = writeZip(tempDir.resolve("volume.zip"), "page.png");
        ZipIterator iterator = new ZipIterator(volume.toFile());
        assertName("page.png", iterator.next());
        Files.delete(volume);

        assertThrows(ImageIteratorException.class, iterator::nextVol);
        iterator.close();
    }

    private static void assertName(String expected, Picture picture) {
        assertNotNull(picture);
        assertEquals(expected, picture.metadata().name());
    }
}
