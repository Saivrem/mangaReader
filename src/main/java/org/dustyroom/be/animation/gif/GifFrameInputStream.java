package org.dustyroom.be.animation.gif;

import java.io.IOException;
import java.io.InputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

final class GifFrameInputStream extends MemoryCacheImageInputStream {
    private long lastSeek;

    GifFrameInputStream(InputStream input) {
        super(input);
    }

    void discardPreviousFrames() throws IOException {
        flushBefore(lastSeek);
    }

    @Override
    public void seek(long position) throws IOException {
        super.seek(position);
        lastSeek = position;
    }
}
