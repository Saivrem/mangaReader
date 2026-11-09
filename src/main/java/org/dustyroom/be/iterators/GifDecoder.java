package org.dustyroom.be.iterators;

import org.dustyroom.be.models.AnimationClip;
import org.w3c.dom.Node;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/** Applies GIF offsets and disposal while holding at most 64 MiB of decoded frames. */
final class GifDecoder {
    private GifDecoder() {}

    static BufferedImage firstFrame(javax.imageio.ImageReader reader) throws IOException {
        Node metadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
        Node screen = child(metadata, "LogicalScreenDescriptor");
        int width = integer(screen, "logicalScreenWidth"), height = integer(screen, "logicalScreenHeight");
        if (width <= 0 || height <= 0 || (long) width * height * 4 > 64L * 1024 * 1024)
            throw new IOException("GIF canvas exceeds the 64 MiB limit");
        Node frameMetadata = reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0");
        Node descriptor = child(frameMetadata, "ImageDescriptor");
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (!"TRUE".equals(attribute(child(frameMetadata, "GraphicControlExtension"), "transparentColorFlag")))
            fill(canvas, 0, 0, width, height, background(metadata));
        Graphics2D g = canvas.createGraphics();
        try { g.drawImage(reader.read(0), integer(descriptor, "imageLeftPosition"), integer(descriptor, "imageTopPosition"), null); }
        finally { g.dispose(); }
        return canvas;
    }

    static AnimationClip read(InputStream input) throws IOException {
        try (var stream = ImageIO.createImageInputStream(input)) {
            var reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.setInput(stream, false, false);
                int count = reader.getNumImages(true);
                if (count <= 1) return null;
                Node metadata = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                Node screen = child(metadata, "LogicalScreenDescriptor");
                int width = integer(screen, "logicalScreenWidth"), height = integer(screen, "logicalScreenHeight");
                if (count > 500 || width <= 0 || height <= 0 || (long) width * height * 4 * (count + 2) > 64L * 1024 * 1024)
                    throw new IOException("GIF animation exceeds the 64 MiB / 500-frame limit; showing its first frame");
                BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                var frames = new ArrayList<BufferedImage>();
                var delays = new ArrayList<Integer>();
                int loops = 1;
                Color background = background(metadata);
                for (int i = 0; i < count; i++) {
                    Node frameMetadata = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    Node control = child(frameMetadata, "GraphicControlExtension");
                    Node descriptor = child(frameMetadata, "ImageDescriptor");
                    int x = integer(descriptor, "imageLeftPosition"), y = integer(descriptor, "imageTopPosition");
                    boolean transparent = "TRUE".equals(attribute(control, "transparentColorFlag"));
                    String disposal = attribute(control, "disposalMethod");
                    if (i == 0 && !transparent) fill(canvas, 0, 0, width, height, background);
                    BufferedImage previous = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                    BufferedImage frame = reader.read(i);
                    Graphics2D g = canvas.createGraphics();
                    try { g.drawImage(frame, x, y, null); } finally { g.dispose(); }
                    frames.add(copy(canvas));
                    int delay = integer(control, "delayTime") * 10;
                    delays.add(delay == 0 ? 100 : Math.max(20, delay));
                    Node extensions = child(frameMetadata, "ApplicationExtensions");
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
                }
                return new AnimationClip(frames, delays, loops);
            } finally { reader.dispose(); }
        }
    }

    private static Color background(Node metadata) {
        Node table = child(metadata, "GlobalColorTable");
        if (table == null) return null;
        int index = integer(table, "backgroundColorIndex");
        for (Node entry = table.getFirstChild(); entry != null; entry = entry.getNextSibling())
            if (integer(entry, "index") == index) return new Color(integer(entry, "red"), integer(entry, "green"), integer(entry, "blue"));
        return null;
    }
    private static BufferedImage copy(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try { g.drawImage(image, 0, 0, null); } finally { g.dispose(); }
        return result;
    }
    private static void fill(BufferedImage image, int x, int y, int width, int height, Color color) {
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(color == null ? AlphaComposite.Clear : AlphaComposite.Src);
            g.setColor(color == null ? Color.BLACK : color);
            g.fillRect(x, y, width, height);
        } finally { g.dispose(); }
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
