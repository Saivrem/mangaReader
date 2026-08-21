package org.dustyroom.ui.window;

import java.awt.Rectangle;
import java.util.Objects;

/** Package-private mutable state; all host-window mutations remain in the controller. */
final class WindowStateModel {
    private WindowMode mode = WindowMode.NORMAL;
    private Rectangle normalBounds;
    private WindowMode modeBeforeFullscreen;
    private Rectangle boundsBeforeFullscreen;

    WindowMode mode() {
        return mode;
    }

    void maximize(Rectangle currentBounds) {
        requireMode(WindowMode.NORMAL);
        normalBounds = copy(currentBounds);
        mode = WindowMode.MAXIMIZED;
    }

    Rectangle restoreFromMaximized() {
        requireMode(WindowMode.MAXIMIZED);
        Rectangle restored = copy(Objects.requireNonNull(normalBounds, "normalBounds"));
        mode = WindowMode.NORMAL;
        return restored;
    }

    void enterFullscreen(Rectangle currentBounds) {
        if (mode == WindowMode.FULLSCREEN) {
            return;
        }
        if (mode == WindowMode.NORMAL) {
            normalBounds = copy(currentBounds);
        }
        modeBeforeFullscreen = mode;
        boundsBeforeFullscreen = copy(currentBounds);
        mode = WindowMode.FULLSCREEN;
    }

    FullscreenRestore exitFullscreen() {
        requireMode(WindowMode.FULLSCREEN);
        WindowMode restoredMode = Objects.requireNonNull(modeBeforeFullscreen, "modeBeforeFullscreen");
        Rectangle restoredBounds = copy(Objects.requireNonNull(boundsBeforeFullscreen, "boundsBeforeFullscreen"));
        mode = restoredMode;
        modeBeforeFullscreen = null;
        boundsBeforeFullscreen = null;
        return new FullscreenRestore(restoredMode, restoredBounds);
    }

    private void requireMode(WindowMode expected) {
        if (mode != expected) {
            throw new IllegalStateException("Expected mode " + expected + " but was " + mode);
        }
    }

    private static Rectangle copy(Rectangle rectangle) {
        return new Rectangle(Objects.requireNonNull(rectangle, "rectangle"));
    }

    record FullscreenRestore(WindowMode mode, Rectangle bounds) {
        FullscreenRestore {
            bounds = new Rectangle(bounds);
        }

        @Override
        public Rectangle bounds() {
            return new Rectangle(bounds);
        }
    }
}
