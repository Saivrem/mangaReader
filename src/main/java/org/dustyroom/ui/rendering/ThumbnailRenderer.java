package org.dustyroom.ui.rendering;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ThumbnailRenderer {
    public static final int LOGICAL_WIDTH = 144;
    public static final int LOGICAL_HEIGHT = 192;
    // Retain enough samples for Retina and fractional desktop scaling, independently of the current monitor.
    public static final int PIXEL_DENSITY = 3;

    public static BufferedImage render(BufferedImage image) {
        double scale = Math.min(1.0, Math.min((double) LOGICAL_WIDTH * PIXEL_DENSITY / image.getWidth(),
                (double) LOGICAL_HEIGHT * PIXEL_DENSITY / image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage current = image;
        // Large single-step reductions discard thin ink lines and alias screentones.
        // Each intermediate reduction is at most 2:1, averaging detail before the final resample.
        do {
            int nextWidth = Math.max(width, current.getWidth() / 2);
            int nextHeight = Math.max(height, current.getHeight() / 2);
            BufferedImage next = new BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = next.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        nextWidth == width && nextHeight == height
                                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(current, 0, 0, nextWidth, nextHeight, null);
            } finally {
                graphics.dispose();
            }
            current = next;
        } while (current.getWidth() != width || current.getHeight() != height);
        return current;
    }
}
