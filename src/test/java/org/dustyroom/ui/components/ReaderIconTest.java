package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderIconTest {
    @Test
    void paintsGridMenuIconWithoutAnOwningComponent() {
        ReaderIcon icon = new ReaderIcon(ReaderIconKind.PAGE_GRID, 24);
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        assertTrue(image.getColorModel().getAlpha(image.getRGB(12, 12)) > 0);
    }
}
