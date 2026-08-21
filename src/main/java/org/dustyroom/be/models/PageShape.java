package org.dustyroom.be.models;

import java.awt.image.BufferedImage;

public record PageShape(int width, int height) {
    public PageShape {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Page dimensions must be positive");
        }
    }

    public static PageShape from(BufferedImage image) {
        return new PageShape(image.getWidth(), image.getHeight());
    }

    public boolean isLandscape() {
        return width > height;
    }
}
