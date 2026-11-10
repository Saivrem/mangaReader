package org.dustyroom.be.iterators;

import org.dustyroom.be.animation.AnimationDecoder;
import org.dustyroom.be.animation.gif.GifDecoder;
import org.dustyroom.be.models.animation.AnimationClip;
import org.dustyroom.be.models.animation.AnimationFrame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
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
        AnimationClip clip = GifDecoder.read(() -> new ByteArrayInputStream(gif));
        try (AnimationDecoder decoder = clip.getOpener().open()) {
            AnimationFrame first = decoder.next();
            AnimationFrame second = decoder.next();
            AnimationFrame third = decoder.next();
            assertEquals(Color.BLUE.getRGB(), second.getImage().getRGB(1, 1));
            assertEquals(Color.RED.getRGB(), third.getImage().getRGB(1, 1));
            assertEquals(Color.GREEN.getRGB(), third.getImage().getRGB(0, 0));
            assertEquals(100, first.getDelayMillis());
            assertEquals(200, second.getDelayMillis());
            assertEquals(300, third.getDelayMillis());
            assertEquals(3, decoder.loops());
            Assertions.assertNull(decoder.next());
        }
    }

    @Test
    void decodesStandaloneAndArchivedAnimationsAndReleasesArchive() throws Exception {
        byte[] gif = fixture("doNotDispose");
        Path file = Files.write(directory.resolve("animation.gif"), gif);
        try (ImageIterator source = ImageIteratorFactory.create(file.toFile())) {
            AnimationClip clip = source.animation(source.pages().get(0));
            try (AnimationDecoder decoder = clip.getOpener().open()) {
                decoder.next();
                decoder.next();
                assertEquals(Color.BLUE.getRGB(), decoder.next().getImage().getRGB(1, 1));
            }
        }
        Path archive = directory.resolve("animation.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("nested/animation.gif"));
            zip.write(gif);
            zip.closeEntry();
        }
        try (ImageIterator source = ImageIteratorFactory.create(archive.toFile())) {
            AnimationClip clip = source.animation(source.pages().get(0));
            source.close();
            try (AnimationDecoder decoder = clip.getOpener().open()) {
                assertNotNull(decoder.next());
                assertNotNull(decoder.next());
                assertNotNull(decoder.next());
                Assertions.assertNull(decoder.next());
            }
        }
        Files.delete(archive);
    }

    @Test
    void readsMoreThanFiveHundredFramesLazilyAndReopens() throws Exception {
        byte[] gif = fixture("doNotDispose", 650);
        AtomicInteger bytesRead = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        AnimationClip clip = GifDecoder.read(() -> new FilterInputStream(new ByteArrayInputStream(gif)) {
            @Override
            public int read() throws IOException {
                int value = super.read();
                if (value >= 0) bytesRead.incrementAndGet();
                return value;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                int count = in.read(bytes, offset, length);
                if (count > 0) bytesRead.addAndGet(count);
                return count;
            }

            @Override
            public void close() throws IOException {
                closed.incrementAndGet();
                super.close();
            }
        });
        assertEquals(0, bytesRead.get());
        for (int pass = 0; pass < 2; pass++) {
            int start = bytesRead.get();
            try (AnimationDecoder decoder = clip.getOpener().open()) {
                assertNotNull(decoder.next());
                Assertions.assertTrue(bytesRead.get() - start < gif.length / 2);
                for (int i = 1; i < 650; i++) assertNotNull(decoder.next());
                Assertions.assertNull(decoder.next());
            }
        }
        assertEquals(2, closed.get());
    }

    @Test
    void preservesTenMillisecondFrameDelays() throws Exception {
        byte[] gif = fixture("doNotDispose", 3, 1);
        AnimationClip clip = GifDecoder.read(() -> new ByteArrayInputStream(gif));
        try (AnimationDecoder decoder = clip.getOpener().open()) {
            assertEquals(10, decoder.next().getDelayMillis());
            assertEquals(20, decoder.next().getDelayMillis());
            assertEquals(30, decoder.next().getDelayMillis());
        }
    }

    private byte[] fixture(String disposal) throws Exception {
        return fixture(disposal, 3);
    }

    private byte[] fixture(String disposal, int count) throws Exception {
        return fixture(disposal, count, 10);
    }

    private byte[] fixture(String disposal, int count, int delayUnits) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < count; i++) {
                int size = i == 0 ? 4 : i == 1 ? 2 : 1;
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(i == 0 ? Color.RED : i == 1 ? Color.BLUE : Color.GREEN);
                g.fillRect(0, 0, size, size);
                g.dispose();
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
                IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode descriptor = (IIOMetadataNode) tree.getElementsByTagName("ImageDescriptor").item(0);
                descriptor.setAttribute("imageLeftPosition", i == 1 ? "1" : "0");
                descriptor.setAttribute("imageTopPosition", i == 1 ? "1" : "0");
                IIOMetadataNode control = (IIOMetadataNode) tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime", Integer.toString((i + 1) * delayUnits));
                control.setAttribute("disposalMethod", i == 1 ? disposal : "doNotDispose");
                if (i == 0) {
                    IIOMetadataNode extensions = new IIOMetadataNode("ApplicationExtensions");
                    IIOMetadataNode extension = new IIOMetadataNode("ApplicationExtension");
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
