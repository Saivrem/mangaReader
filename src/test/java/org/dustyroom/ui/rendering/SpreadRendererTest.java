package org.dustyroom.ui.rendering;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpreadRendererTest {
    @Test
    void unequalPagePaddingRemainsTransparentForTheActiveThemeCanvas() {
        BufferedImage tallPage = solidImage(2, 4, Color.RED);
        BufferedImage shortPage = solidImage(2, 2, Color.BLUE);

        BufferedImage spread = new SpreadRenderer().compose(tallPage, shortPage, false);

        assertEquals(4, spread.getWidth());
        assertEquals(4, spread.getHeight());
        assertEquals(Color.RED.getRGB(), spread.getRGB(0, 0));
        assertEquals(Color.BLUE.getRGB(), spread.getRGB(3, 1));
        assertEquals(0, new Color(spread.getRGB(3, 0), true).getAlpha());
        assertEquals(0, new Color(spread.getRGB(3, 3), true).getAlpha());
    }

    private static BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
