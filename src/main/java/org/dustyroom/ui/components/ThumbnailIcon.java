package org.dustyroom.ui.components;

import lombok.Getter;

import org.dustyroom.ui.rendering.ThumbnailRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Logical size stays constant; Java2D uses the retained pixels at the monitor's device scale.
 */
public final class ThumbnailIcon implements Icon {
    private final BufferedImage image;
    @Getter
    private final int iconWidth;
    @Getter
    private final int iconHeight;

    public ThumbnailIcon(BufferedImage image) {
        this.image = image;
        double scale = Math.min(1.0, Math.min((double) ThumbnailRenderer.LOGICAL_WIDTH / image.getWidth(),
                (double) ThumbnailRenderer.LOGICAL_HEIGHT / image.getHeight()));
        iconWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        iconHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            copy.drawImage(image, x, y, iconWidth, iconHeight, null);
        } finally {
            copy.dispose();
        }
    }
}
