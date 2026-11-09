package org.dustyroom.be.iterators;

import org.dustyroom.be.models.AnimationClip;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Sequential GIF composition. Consumed compressed bytes and prior frames are discarded.
 */
final class GifDecoder {
    private GifDecoder() {
    }

    static BufferedImage firstFrame(javax.imageio.ImageReader reader) throws IOException {
        Node metadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
        Node screen = child(metadata, "LogicalScreenDescriptor");
        int width = integer(screen, "logicalScreenWidth"), height = integer(screen, "logicalScreenHeight");
        if (width <= 0 || height <= 0) throw new IOException("Invalid GIF canvas");
        Node frameMetadata = reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0");
        Node descriptor = child(frameMetadata, "ImageDescriptor");
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (!"TRUE".equals(attribute(child(frameMetadata, "GraphicControlExtension"), "transparentColorFlag")))
            fill(canvas, 0, 0, width, height, background(metadata));
        Graphics2D g = canvas.createGraphics();
        try {
            g.drawImage(reader.read(0), integer(descriptor, "imageLeftPosition"), integer(descriptor, "imageTopPosition"), null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    @FunctionalInterface
    interface InputFactory { InputStream open() throws IOException; }

    static AnimationClip read(InputFactory inputs) {
        return new AnimationClip(() -> new Sequence(inputs.open()));
    }

    // ImageIO revisits the current frame header to locate the next one. Retain that
    // frame's compressed bytes, not the full file, and discard everything before it.
    private static final class ForwardCache extends MemoryCacheImageInputStream {
        private long lastSeek;
        ForwardCache(InputStream input) { super(input); }
        @Override public void seek(long position) throws IOException {
            super.seek(position);
            lastSeek = position;
        }
    }

    private static final class Sequence implements AnimationClip.Decoder {
        private final InputStream input;
        private final ForwardCache stream;
        private final javax.imageio.ImageReader reader;
        private BufferedImage canvas;
        private Color background;
        private int index, loops = 1;

        Sequence(InputStream input) throws IOException {
            this.input = input;
            stream = new ForwardCache(input);
            reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.addIIOReadProgressListener(new javax.imageio.event.IIOReadProgressListener() {
                    public void imageProgress(javax.imageio.ImageReader source, float progress) {
                        if (Thread.currentThread().isInterrupted()) source.abort();
                    }
                    public void sequenceStarted(javax.imageio.ImageReader source, int index) {}
                    public void sequenceComplete(javax.imageio.ImageReader source) {}
                    public void imageStarted(javax.imageio.ImageReader source, int index) {}
                    public void imageComplete(javax.imageio.ImageReader source) {}
                    public void thumbnailStarted(javax.imageio.ImageReader source, int image, int thumbnail) {}
                    public void thumbnailProgress(javax.imageio.ImageReader source, float progress) {}
                    public void thumbnailComplete(javax.imageio.ImageReader source) {}
                    public void readAborted(javax.imageio.ImageReader source) {}
                });
                reader.setInput(stream, true, false);
                Node metadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                Node screen = child(metadata, "LogicalScreenDescriptor");
                int width = integer(screen, "logicalScreenWidth"), height = integer(screen, "logicalScreenHeight");
                if (width <= 0 || height <= 0) throw new IOException("Invalid GIF canvas");
                canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                background = background(metadata);
            } catch (IOException | RuntimeException error) {
                try { close(); } catch (IOException suppressed) { error.addSuppressed(suppressed); }
                throw error;
            }
        }

        @Override public AnimationClip.Frame next() throws IOException {
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
            if (index == 0 && !transparent) fill(canvas, 0, 0, canvas.getWidth(), canvas.getHeight(), background);
            BufferedImage previous = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
            BufferedImage frame = reader.read(index++);
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("GIF playback cancelled");
            Graphics2D g = canvas.createGraphics();
            try { g.drawImage(frame, x, y, null); } finally { g.dispose(); }
            BufferedImage composed = copy(canvas);
            int delay = integer(control, "delayTime") * 10;
            Node extensions = child(metadata, "ApplicationExtensions");
            if (extensions != null) for (Node extension = extensions.getFirstChild(); extension != null; extension = extension.getNextSibling()) {
                if ("NETSCAPE".equals(attribute(extension, "applicationID"))
                        && extension instanceof IIOMetadataNode node && node.getUserObject() instanceof byte[] bytes
                        && bytes.length >= 3 && bytes[0] == 1) {
                    int repeats = (bytes[1] & 255) | ((bytes[2] & 255) << 8);
                    loops = repeats == 0 ? 0 : repeats + 1;
                }
            }
            if (previous != null) canvas = previous;
            else if ("restoreToBackgroundColor".equals(disposal))
                fill(canvas, x, y, frame.getWidth(), frame.getHeight(), transparent ? null : background);
            // Forward-only reading: do not retain compressed data for the entire animation.
            stream.flushBefore(stream.lastSeek);
            return new AnimationClip.Frame(composed, delay == 0 ? 100 : Math.max(20, delay));
        }

        @Override public int loops() { return loops; }
        @Override public void close() throws IOException {
            reader.dispose();
            canvas = null;
            try { stream.close(); } finally { input.close(); }
        }
    }

    private static Color background(Node metadata) {
        Node table = child(metadata, "GlobalColorTable");
        if (table == null) return null;
        int index = integer(table, "backgroundColorIndex");
        for (Node entry = table.getFirstChild(); entry != null; entry = entry.getNextSibling())
            if (integer(entry, "index") == index)
                return new Color(integer(entry, "red"), integer(entry, "green"), integer(entry, "blue"));
        return null;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, Color color) {
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(color == null ? AlphaComposite.Clear : AlphaComposite.Src);
            g.setColor(color == null ? Color.BLACK : color);
            g.fillRect(x, y, width, height);
        } finally {
            g.dispose();
        }
    }

    private static Node child(Node parent, String name) {
        if (parent != null) for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node.getNodeName().equals(name)) return node;
        return null;
    }

    private static String attribute(Node node, String name) {
        Node value = node == null ? null : node.getAttributes().getNamedItem(name);
        return value == null ? "" : value.getNodeValue();
    }

    private static int integer(Node node, String name) {
        String value = attribute(node, name);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }
}
