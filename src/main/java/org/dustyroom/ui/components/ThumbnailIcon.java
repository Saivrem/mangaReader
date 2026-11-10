package org.dustyroom.ui.components;

import org.dustyroom.ui.rendering.ThumbnailRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Logical size stays constant; Java2D uses the retained pixels at the monitor's device scale.
 */
public final class ThumbnailIcon implements Icon {
    private final BufferedImage image;
    private final int width;
    private final int height;

    public ThumbnailIcon(BufferedImage image) {
        this.image = image;
        double scale = Math.min(1.0, Math.min((double) ThumbnailRenderer.LOGICAL_WIDTH / image.getWidth(),
                (double) ThumbnailRenderer.LOGICAL_HEIGHT / image.getHeight()));
        width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        height = Math.max(1, (int) Math.round(image.getHeight() * scale));
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            copy.drawImage(image, x, y, width, height, null);
        } finally {
            copy.dispose();
        }
    }
}
