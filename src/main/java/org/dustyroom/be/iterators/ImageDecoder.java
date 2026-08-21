package org.dustyroom.be.iterators;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Decodes images while handling JPEGs whose JFIF and Adobe color markers
 * cause the JDK reader to expose only a raw YCbCr raster.
 */
final class ImageDecoder {
    private static final String JPEG_FORMAT = "JPEG";
    private static final int JPEG_START_OF_IMAGE = 0xffd8;
    private static final int JPEG_START_OF_SCAN = 0xda;
    private static final int JPEG_END_OF_IMAGE = 0xd9;
    private static final int JPEG_APP0 = 0xe0;
    private static final byte[] JFIF_SIGNATURE = {'J', 'F', 'I', 'F', 0};

    private ImageDecoder() {
    }

    static BufferedImage read(InputStream input) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                boolean jfif = isJpeg(reader) && hasJfifMarker(imageInput);
                reader.setInput(imageInput, false, false);
                if (jfif && !reader.getImageTypes(0).hasNext()) {
                    return readUnsupportedJfif(reader);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage readUnsupportedJfif(ImageReader reader) throws IOException {
        Raster raster = reader.readRaster(0, null);
        if (raster.getNumBands() != 3) {
            throw new IOException("Unsupported JPEG raster with " + raster.getNumBands() + " color bands");
        }
        return yCbCrToRgb(raster);
    }

    private static boolean isJpeg(ImageReader reader) throws IOException {
        return JPEG_FORMAT.equalsIgnoreCase(reader.getFormatName());
    }

    private static boolean hasJfifMarker(ImageInputStream input) throws IOException {
        long originalPosition = input.getStreamPosition();
        try {
            input.seek(0);
            if (input.readUnsignedShort() != JPEG_START_OF_IMAGE) {
                return false;
            }

            while (true) {
                int marker = nextMarker(input);
                if (marker < 0 || marker == JPEG_START_OF_SCAN || marker == JPEG_END_OF_IMAGE) {
                    return false;
                }
                if (isStandaloneMarker(marker)) {
                    continue;
                }

                int length = input.readUnsignedShort();
                if (length < 2) {
                    return false;
                }
                long segmentEnd = input.getStreamPosition() + length - 2L;
                if (marker == JPEG_APP0 && length >= JFIF_SIGNATURE.length + 2) {
                    boolean matches = true;
                    for (byte expected : JFIF_SIGNATURE) {
                        if (input.read() != (expected & 0xff)) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        return true;
                    }
                }
                input.seek(segmentEnd);
            }
        } finally {
            input.seek(originalPosition);
        }
    }

    private static int nextMarker(ImageInputStream input) throws IOException {
        int value;
        do {
            value = input.read();
        } while (value >= 0 && value != 0xff);
        if (value < 0) {
            return -1;
        }
        do {
            value = input.read();
        } while (value == 0xff);
        return value;
    }

    private static boolean isStandaloneMarker(int marker) {
        return marker == 0x01 || marker >= 0xd0 && marker <= 0xd8;
    }

    private static BufferedImage yCbCrToRgb(Raster source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] destination = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int[] row = new int[width * 3];
        int minX = source.getMinX();
        int minY = source.getMinY();

        for (int y = 0; y < height; y++) {
            source.getPixels(minX, minY + y, width, 1, row);
            int destinationOffset = y * width;
            for (int x = 0, sample = 0; x < width; x++, sample += 3) {
                int luminance = row[sample];
                int blueDifference = row[sample + 1] - 128;
                int redDifference = row[sample + 2] - 128;

                int red = clamp(luminance + ((91881 * redDifference) >> 16));
                int green = clamp(luminance - ((22554 * blueDifference + 46802 * redDifference) >> 16));
                int blue = clamp(luminance + ((116130 * blueDifference) >> 16));
                destination[destinationOffset + x] = red << 16 | green << 8 | blue;
            }
        }
        return image;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
