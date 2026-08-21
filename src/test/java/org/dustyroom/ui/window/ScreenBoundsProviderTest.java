package org.dustyroom.ui.window;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenBoundsProviderTest {
    @Test
    void defaultSelectionUsesPointerAndSupportsLightweightFakes() {
        ScreenBoundsProvider provider = () -> List.of(
                new ScreenBoundsProvider.ScreenBounds(
                        null,
                        new Rectangle(-1600, 0, 1600, 900),
                        new Rectangle(-1600, 24, 1600, 876)
                ),
                new ScreenBoundsProvider.ScreenBounds(
                        null,
                        new Rectangle(0, 0, 1920, 1080),
                        new Rectangle(0, 0, 1920, 1040)
                )
        );

        ScreenBoundsProvider.ScreenBounds selected = provider.screenFor(
                new Point(-200, 100),
                new Rectangle(100, 100, 800, 600)
        );

        assertEquals(new Rectangle(-1600, 24, 1600, 876), selected.usableBounds());
    }

    @Test
    void screenBoundsAreDefensivelyCopied() {
        Rectangle physical = new Rectangle(0, 0, 1920, 1080);
        Rectangle usable = new Rectangle(0, 0, 1920, 1040);
        ScreenBoundsProvider.ScreenBounds screen = new ScreenBoundsProvider.ScreenBounds(null, physical, usable);

        physical.width = 1;
        usable.height = 1;
        Rectangle returned = screen.bounds();
        returned.width = 2;

        assertEquals(new Rectangle(0, 0, 1920, 1080), screen.bounds());
        assertEquals(new Rectangle(0, 0, 1920, 1040), screen.usableBounds());
    }
}
