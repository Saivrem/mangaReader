package org.dustyroom.be.iterators;

import java.io.File;

import static org.dustyroom.be.utils.FileUtils.*;

public final class ImageIteratorFactory {
    private ImageIteratorFactory() {
    }

    public static ImageIterator create(File source) {
        if (isZipFile.test(source)) {
            return new ZipIterator(source);
        }
        if (isImage.test(source) || containsImages.test(source)) {
            return new FileImageIterator(source);
        }
        throw new ImageIteratorException("Unsupported or empty image source", source);
    }
}
