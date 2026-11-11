package org.dustyroom.be.animation.gif;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.dustyroom.be.animation.AnimationDecoder;
import org.dustyroom.be.models.animation.AnimationFrame;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import static org.dustyroom.be.animation.gif.GifDecoder.*;

final class GifSequenceDecoder implements AnimationDecoder {
    private final InputStream input;
    private final GifFrameInputStream stream;
    private final ImageReader reader;
    private final Color background;
    private BufferedImage canvas;
    private int index;
    @Getter
    @Accessors(fluent = true)
    private int loops = 1;

    GifSequenceDecoder(InputStream input) throws IOException {
        this.input = input;
        stream = new GifFrameInputStream(input);
        reader = ImageIO.getImageReadersByFormatName("gif").next();
        try {
            reader.addIIOReadProgressListener(new GifCancellationListener());
            reader.setInput(stream, true, false);
            Node metadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
            Node screen = child(metadata, "LogicalScreenDescriptor");
            int width = integer(screen, "logicalScreenWidth"), height = integer(screen, "logicalScreenHeight");
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid GIF canvas");
            }
            canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            background = background(metadata);
        } catch (IOException | RuntimeException error) {
            try {
                close();
            } catch (IOException suppressed) {
                error.addSuppressed(suppressed);
            }
            throw error;
        }
    }

    @Override
    public AnimationFrame next() throws IOException {
        Node metadata;
        try {
            metadata = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
        } catch (IndexOutOfBoundsException end) {
            return null;
        }
        Node control = child(metadata, "GraphicControlExtension");
        Node descriptor = child(metadata, "ImageDescriptor");
        int x = integer(descriptor, "imageLeftPosition"), y = integer(descriptor, "imageTopPosition");
        boolean transparent = "TRUE".equals(attribute(control, "transparentColorFlag"));
        String disposal = attribute(control, "disposalMethod");
        if (index == 0 && !transparent) {
            fill(canvas, 0, 0, canvas.getWidth(), canvas.getHeight(), background);
        }
        BufferedImage previous = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
        BufferedImage frame = reader.read(index++);
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("GIF playback cancelled");
        }
        Graphics2D g = canvas.createGraphics();
        try {
            g.drawImage(frame, x, y, null);
        } finally {
            g.dispose();
        }
        BufferedImage composed = copy(canvas);
        int delay = integer(control, "delayTime") * 10;
        Node extensions = child(metadata, "ApplicationExtensions");
        if (extensions != null) {
            for (Node extension = extensions.getFirstChild(); extension != null; extension = extension.getNextSibling()) {
                if ("NETSCAPE".equals(attribute(extension, "applicationID"))
                        && extension instanceof IIOMetadataNode node && node.getUserObject() instanceof byte[] bytes
                        && bytes.length >= 3 && bytes[0] == 1) {
                    int repeats = (bytes[1] & 255) | ((bytes[2] & 255) << 8);
                    loops = repeats == 0 ? 0 : repeats + 1;
                }
            }
        }
        if (previous != null) {
            canvas = previous;
        } else if ("restoreToBackgroundColor".equals(disposal)) {
            fill(canvas, x, y, frame.getWidth(), frame.getHeight(), transparent ? null : background);
        }
        // Forward-only reading: do not retain compressed data for the entire animation.
        stream.discardPreviousFrames();
        return new AnimationFrame(composed, delay == 0 ? 100 : delay);
    }

    @Override
    public void close() throws IOException {
        reader.dispose();
        canvas = null;
        try {
            stream.close();
        } finally {
            input.close();
        }
    }
}
