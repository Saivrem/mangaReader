package org.dustyroom.be.iterators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.dustyroom.be.iterators.ImageTestFixtures.writeImage;
import static org.dustyroom.be.iterators.ImageTestFixtures.writeZip;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageIteratorFactoryTest {
    @TempDir
    Path tempDir;

    @Test
    void routesImagesDirectoriesAndArchivesCaseInsensitively() throws Exception {
        Path image = writeImage(tempDir.resolve("chapter"), "page.PNG");
        Path archive = writeZip(tempDir.resolve("volume.ZIP"), "page.png");

        try (ImageIterator imageIterator = ImageIteratorFactory.create(image.toFile());
             ImageIterator directoryIterator = ImageIteratorFactory.create(image.getParent().toFile());
             ImageIterator archiveIterator = ImageIteratorFactory.create(archive.toFile())) {
            assertInstanceOf(FileImageIterator.class, imageIterator);
            assertInstanceOf(FileImageIterator.class, directoryIterator);
            assertInstanceOf(ZipIterator.class, archiveIterator);
        }
    }

    @Test
    void rejectsUnsupportedFilesAndDirectoriesWithoutImages() throws Exception {
        Path text = Files.writeString(tempDir.resolve("notes.txt"), "notes");
        Path emptyDirectory = Files.createDirectory(tempDir.resolve("empty"));

        assertThrows(ImageIteratorException.class, () -> ImageIteratorFactory.create(text.toFile()));
        assertThrows(ImageIteratorException.class, () -> ImageIteratorFactory.create(emptyDirectory.toFile()));
    }
}
