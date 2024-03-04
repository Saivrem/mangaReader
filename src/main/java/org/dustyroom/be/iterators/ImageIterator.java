package org.dustyroom.be.iterators;

import java.awt.image.BufferedImage;

public interface ImageIterator {

    BufferedImage next();

    BufferedImage prev();

    BufferedImage first();

    BufferedImage last();
}
