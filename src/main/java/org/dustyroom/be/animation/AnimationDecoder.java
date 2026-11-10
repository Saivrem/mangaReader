package org.dustyroom.be.animation;

import java.io.IOException;
import org.dustyroom.be.models.animation.AnimationFrame;

/**
 * Sequential decoder for one pass through an animation.
 */
public interface AnimationDecoder extends AutoCloseable {
    /**
     * Next composited frame, or null at the end of one pass.
     */
    AnimationFrame next() throws IOException;

    /**
     * Total passes; zero means repeat forever.
     */
    int loops();

    @Override
    void close() throws IOException;
}
