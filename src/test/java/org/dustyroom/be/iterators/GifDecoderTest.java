package org.dustyroom.be.iterators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class GifDecoderTest {
    @TempDir Path directory;

    @Test void compositesOffsetsDisposalTimingAndFiniteLoopCount() throws Exception {
        byte[] gif = fixture("restoreToPrevious");
        var clip = GifDecoder.read(new ByteArrayInputStream(gif));
        assertNotNull(clip);
        assertEquals(3, clip.frames().size());
        assertEquals(Color.BLUE.getRGB(), clip.frames().get(1).getRGB(1, 1));
        assertEquals(Color.RED.getRGB(), clip.frames().get(2).getRGB(1, 1));
        assertEquals(Color.GREEN.getRGB(), clip.frames().get(2).getRGB(0, 0));
        assertEquals(java.util.List.of(100, 200, 300), clip.delays());
        assertEquals(3, clip.loops());
        assertEquals(0, clip.frameAt(0));
        assertEquals(1, clip.frameAt(100));
        assertEquals(2, clip.frameAt(300));
        assertEquals(0, clip.frameAt(600));
        assertEquals(2, clip.frameAt(1800));
    }

    @Test void decodesStandaloneAndArchivedAnimationsAndReleasesArchive() throws Exception {
        byte[] gif = fixture("doNotDispose");
        Path file = Files.write(directory.resolve("animation.gif"), gif);
        try (var source = ImageIteratorFactory.create(file.toFile())) {
            var clip = source.animation(source.pages().get(0));
            assertEquals(Color.BLUE.getRGB(), clip.frames().get(2).getRGB(1, 1));
        }
        Path archive = directory.resolve("animation.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("nested/animation.gif"));
            zip.write(gif);
            zip.closeEntry();
        }
        try (var source = ImageIteratorFactory.create(archive.toFile())) {
            assertEquals(3, source.animation(source.pages().get(0)).frames().size());
        }
        Files.delete(archive);
    }

    private byte[] fixture(String disposal) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < 3; i++) {
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
        } finally { writer.dispose(); }
        return output.toByteArray();
    }
}
