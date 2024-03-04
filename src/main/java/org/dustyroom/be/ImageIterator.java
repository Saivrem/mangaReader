package org.dustyroom.be;

import java.awt.image.BufferedImage;

public interface ImageIterator {

    BufferedImage next();

    BufferedImage prev();

    BufferedImage first();

    BufferedImage last();
}
