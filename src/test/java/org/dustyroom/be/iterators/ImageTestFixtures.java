package org.dustyroom.be.iterators;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ImageTestFixtures {
    private ImageTestFixtures() {
    }

    static Path writeImage(Path directory, String name) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(name);
        ImageIO.write(image(), extension(name), target.toFile());
        return target;
    }

    static byte[] pngBytes() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image(), "png", output);
            return output.toByteArray();
        }
    }

    static Path writeZip(Path target, String... imageEntries) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (String entry : imageEntries) {
            entries.put(entry, pngBytes());
        }
        return writeZip(target, entries);
    }

    static Path writeZip(Path target, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return target;
    }

    private static BufferedImage image() {
        BufferedImage image = new BufferedImage(4, 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        return image;
    }

    private static String extension(String name) {
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
