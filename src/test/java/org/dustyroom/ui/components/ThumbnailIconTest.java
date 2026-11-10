package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailIconTest {
    @Test
    void retinaPaintingUsesPhysicalPixelsInsteadOfStretchingALowResolutionIcon() {
        BufferedImage source = new BufferedImage(288, 384, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) source.setRGB(x, y, x % 2 == 0 ? 0 : 0xffffff);
        }
        ThumbnailIcon icon = new ThumbnailIcon(source);
        assertEquals(144, icon.getIconWidth());
        assertEquals(192, icon.getIconHeight());
        BufferedImage screen = new BufferedImage(288, 384, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = screen.createGraphics();
        try {
            graphics.scale(2, 2);
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        assertTrue((screen.getRGB(100, 100) & 255) < 20);
        assertTrue((screen.getRGB(101, 100) & 255) > 235);
    }
}
