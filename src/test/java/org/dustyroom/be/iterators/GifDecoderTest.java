package org.dustyroom.be.iterators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GifDecoderTest {
    @TempDir
    Path directory;

    @Test
    void compositesOffsetsDisposalTimingAndFiniteLoopCount() throws Exception {
        byte[] gif = fixture("restoreToPrevious");
        var clip = GifDecoder.read(() -> new ByteArrayInputStream(gif));
        try (var decoder = clip.opener().open()) {
            var first = decoder.next();
            var second = decoder.next();
            var third = decoder.next();
            assertEquals(Color.BLUE.getRGB(), second.image().getRGB(1, 1));
            assertEquals(Color.RED.getRGB(), third.image().getRGB(1, 1));
            assertEquals(Color.GREEN.getRGB(), third.image().getRGB(0, 0));
            assertEquals(100, first.delayMillis());
            assertEquals(200, second.delayMillis());
            assertEquals(300, third.delayMillis());
            assertEquals(3, decoder.loops());
            org.junit.jupiter.api.Assertions.assertNull(decoder.next());
        }
    }

    @Test
    void decodesStandaloneAndArchivedAnimationsAndReleasesArchive() throws Exception {
        byte[] gif = fixture("doNotDispose");
        Path file = Files.write(directory.resolve("animation.gif"), gif);
        try (var source = ImageIteratorFactory.create(file.toFile())) {
            var clip = source.animation(source.pages().get(0));
            try (var decoder = clip.opener().open()) {
                decoder.next(); decoder.next();
                assertEquals(Color.BLUE.getRGB(), decoder.next().image().getRGB(1, 1));
            }
        }
        Path archive = directory.resolve("animation.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("nested/animation.gif"));
            zip.write(gif);
            zip.closeEntry();
        }
        try (var source = ImageIteratorFactory.create(archive.toFile())) {
            var clip = source.animation(source.pages().get(0));
            source.close();
            try (var decoder = clip.opener().open()) {
                assertNotNull(decoder.next());
                assertNotNull(decoder.next());
                assertNotNull(decoder.next());
                org.junit.jupiter.api.Assertions.assertNull(decoder.next());
            }
        }
        Files.delete(archive);
    }

    @Test
    void readsMoreThanFiveHundredFramesLazilyAndReopens() throws Exception {
        byte[] gif = fixture("doNotDispose", 650);
        var bytesRead = new java.util.concurrent.atomic.AtomicInteger();
        var closed = new java.util.concurrent.atomic.AtomicInteger();
        var clip = GifDecoder.read(() -> new java.io.FilterInputStream(new ByteArrayInputStream(gif)) {
            @Override public int read() throws java.io.IOException {
                int value = super.read();
                if (value >= 0) bytesRead.incrementAndGet();
                return value;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws java.io.IOException {
                int count = in.read(bytes, offset, length);
                if (count > 0) bytesRead.addAndGet(count);
                return count;
            }
            @Override public void close() throws java.io.IOException { closed.incrementAndGet(); super.close(); }
        });
        assertEquals(0, bytesRead.get());
        for (int pass = 0; pass < 2; pass++) {
            int start = bytesRead.get();
            try (var decoder = clip.opener().open()) {
                assertNotNull(decoder.next());
                org.junit.jupiter.api.Assertions.assertTrue(bytesRead.get() - start < gif.length / 2);
                for (int i = 1; i < 650; i++) assertNotNull(decoder.next());
                org.junit.jupiter.api.Assertions.assertNull(decoder.next());
            }
        }
        assertEquals(2, closed.get());
    }

    private byte[] fixture(String disposal) throws Exception { return fixture(disposal, 3); }

    private byte[] fixture(String disposal, int count) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < count; i++) {
                int size = i == 0 ? 4 : i == 1 ? 2 : 1;
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(i == 0 ? Color.RED : i == 1 ? Color.BLUE : Color.GREEN);
                g.fillRect(0, 0, size, size);
                g.dispose();
                var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
                var tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
                var descriptor = (IIOMetadataNode) tree.getElementsByTagName("ImageDescriptor").item(0);
                descriptor.setAttribute("imageLeftPosition", i == 1 ? "1" : "0");
                descriptor.setAttribute("imageTopPosition", i == 1 ? "1" : "0");
                var control = (IIOMetadataNode) tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime", Integer.toString((i + 1) * 10));
                control.setAttribute("disposalMethod", i == 1 ? disposal : "doNotDispose");
                if (i == 0) {
                    var extensions = new IIOMetadataNode("ApplicationExtensions");
                    var extension = new IIOMetadataNode("ApplicationExtension");
                    extension.setAttribute("applicationID", "NETSCAPE");
                    extension.setAttribute("authenticationCode", "2.0");
                    extension.setUserObject(new byte[]{1, 2, 0});
                    extensions.appendChild(extension);
                    tree.appendChild(extensions);
                }
                metadata.setFromTree("javax_imageio_gif_image_1.0", tree);
                writer.writeToSequence(new IIOImage(image, null, metadata), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }
}
