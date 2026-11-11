package org.dustyroom.be.animation.gif;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

final class GifCancellationListener implements IIOReadProgressListener {

    public void imageProgress(ImageReader source, float progress) {
        if (Thread.currentThread().isInterrupted()) {
            source.abort();
        }
    }

    public void sequenceStarted(ImageReader source, int index) {
    }

    public void sequenceComplete(ImageReader source) {
    }

    public void imageStarted(ImageReader source, int index) {
    }

    public void imageComplete(ImageReader source) {
    }

    public void thumbnailStarted(ImageReader source, int image, int thumbnail) {
    }

    public void thumbnailProgress(ImageReader source, float progress) {
    }

    public void thumbnailComplete(ImageReader source) {
    }

    public void readAborted(ImageReader source) {
    }
}
