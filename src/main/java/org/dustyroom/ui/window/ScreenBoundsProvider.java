package org.dustyroom.ui.window;

import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/** Provides monitor geometry without coupling pure state logic to AWT globals. */
public interface ScreenBoundsProvider {
    List<ScreenBounds> screens();

    default ScreenBounds screenFor(Point pointerOnScreen, Rectangle windowBounds) {
        List<ScreenBounds> availableScreens = screens();
        int index = WindowGeometry.selectScreen(
                availableScreens.stream().map(ScreenBounds::bounds).toList(),
                pointerOnScreen,
                windowBounds
        );
        return availableScreens.get(index);
    }

    static ScreenBoundsProvider system() {
        return new AwtScreenBoundsProvider();
    }

    /**
     * The graphics device is nullable to keep fake providers lightweight and
     * is retained as monitor metadata for platform integrations. The viewer's
     * borderless fullscreen intentionally uses {@link #bounds()} instead of
     * AWT full-screen exclusive mode.
     */
    record ScreenBounds(GraphicsDevice device, Rectangle bounds, Rectangle usableBounds) {
        public ScreenBounds {
            bounds = new Rectangle(bounds);
            usableBounds = new Rectangle(usableBounds);
        }

        @Override
        public Rectangle bounds() {
            return new Rectangle(bounds);
        }

        @Override
        public Rectangle usableBounds() {
            return new Rectangle(usableBounds);
        }
    }
}
