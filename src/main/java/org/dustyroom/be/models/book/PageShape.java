package org.dustyroom.be.models.book;

import java.awt.image.BufferedImage;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class PageShape {
    private int width;
    private int height;

    @Builder
    public PageShape(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Page dimensions must be positive");
        }

        this.width = width;
        this.height = height;
    }

    public static PageShape from(BufferedImage image) {
        return new PageShape(image.getWidth(), image.getHeight());
    }

    public boolean isLandscape() {
        return width > height;
    }
    public PageShape setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Page dimensions must be positive");
        }
        this.width = width;
        return this;
    }

    public PageShape setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Page dimensions must be positive");
        }
        this.height = height;
        return this;
    }
}
