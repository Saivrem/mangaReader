package org.dustyroom.be.iterators;

import org.dustyroom.be.StringComparator;
import org.dustyroom.be.models.Picture;

public interface ImageIterator {
    StringComparator stringComparator = new StringComparator();

    Picture next();

    Picture prev();

    Picture first();

    Picture last();
}
