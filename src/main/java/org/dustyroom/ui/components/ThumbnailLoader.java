package org.dustyroom.ui.components;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import org.dustyroom.be.models.book.PageRef;

@FunctionalInterface
public interface ThumbnailLoader {
    void load(int index, PageRef page, Consumer<BufferedImage> result);
}
