package org.dustyroom.be.animation;

import java.io.IOException;

@FunctionalInterface
public interface AnimationOpener {
    AnimationDecoder open() throws IOException;
}
