package org.dustyroom.ui.components;

import org.dustyroom.be.models.PageRef;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

@FunctionalInterface
public interface ThumbnailLoader {
    void load(int index, PageRef page, Consumer<BufferedImage> result);
}
