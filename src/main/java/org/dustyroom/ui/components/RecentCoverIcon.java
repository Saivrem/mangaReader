package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Paints a cached cover without resizing the original image.
 */
final class RecentCoverIcon implements Icon {
    private final BufferedImage image;

    RecentCoverIcon(BufferedImage image) {
        this.image = image;
    }

    @Override
    public int getIconWidth() {
        return 72;
    }

    @Override
    public int getIconHeight() {
        return 104;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        double scale = Math.min(72.0 / image.getWidth(), 104.0 / image.getHeight());
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            copy.drawImage(image, x + (72 - width) / 2, y + (104 - height) / 2, width, height, null);
        } finally {
            copy.dispose();
        }
    }
}
