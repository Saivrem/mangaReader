package org.dustyroom.be.models;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** A reopenable animation source; owns no open files or decoded frame collection. */
public record AnimationClip(Opener opener) {
    @FunctionalInterface
    public interface Opener { Decoder open() throws IOException; }

    public interface Decoder extends AutoCloseable {
        /** Next composited frame, or null at the end of one pass. */
        Frame next() throws IOException;
        /** Total passes; zero means repeat forever. */
        int loops();
        @Override void close() throws IOException;
    }

    public record Frame(BufferedImage image, int delayMillis) {}
}
