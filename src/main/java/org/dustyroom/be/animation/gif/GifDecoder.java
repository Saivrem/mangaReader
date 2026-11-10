package org.dustyroom.be.animation.gif;

import org.dustyroom.be.animation.AnimationClip;
import org.w3c.dom.Node;

import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Sequential GIF composition. Consumed compressed bytes and prior frames are discarded.
 */
public final class GifDecoder {
    private GifDecoder() {
    }

    public static BufferedImage firstFrame(ImageReader reader) throws IOException {
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

    public static AnimationClip read(GifInputFactory inputs) {
        return new AnimationClip(() -> new GifSequenceDecoder(inputs.open()));
    }

    // ImageIO revisits the current frame header to locate the next one. Retain that
    // frame's compressed bytes, not the full file, and discard everything before it.

    static Color background(Node metadata) {
        Node table = child(metadata, "GlobalColorTable");
        if (table == null) return null;
        int index = integer(table, "backgroundColorIndex");
        for (Node entry = table.getFirstChild(); entry != null; entry = entry.getNextSibling())
            if (integer(entry, "index") == index)
                return new Color(integer(entry, "red"), integer(entry, "green"), integer(entry, "blue"));
        return null;
    }

    static BufferedImage copy(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    static void fill(BufferedImage image, int x, int y, int width, int height, Color color) {
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(color == null ? AlphaComposite.Clear : AlphaComposite.Src);
            g.setColor(color == null ? Color.BLACK : color);
            g.fillRect(x, y, width, height);
        } finally {
            g.dispose();
        }
    }

    static Node child(Node parent, String name) {
        if (parent != null) for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node.getNodeName().equals(name)) return node;
        return null;
    }

    static String attribute(Node node, String name) {
        Node value = node == null ? null : node.getAttributes().getNamedItem(name);
        return value == null ? "" : value.getNodeValue();
    }

    static int integer(Node node, String name) {
        String value = attribute(node, name);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }
}
