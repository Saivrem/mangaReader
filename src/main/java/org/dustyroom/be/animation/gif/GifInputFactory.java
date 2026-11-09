package org.dustyroom.be.animation.gif;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface GifInputFactory {
    InputStream open() throws IOException;
}
