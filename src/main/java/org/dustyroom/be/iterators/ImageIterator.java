package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Picture;

public interface ImageIterator {

    Picture next();

    Picture prev();

    Picture first();

    Picture last();
}
