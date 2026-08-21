package org.dustyroom.ui.window;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowGeometryTest {
    @Test
    void computesUsableBoundsWithInsetsAndNegativeCoordinates() {
        Rectangle usable = WindowGeometry.usableBounds(
                new Rectangle(-1920, 0, 1920, 1080),
                new Insets(24, 0, 40, 12)
        );

        assertEquals(new Rectangle(-1920, 24, 1908, 1016), usable);
    }

    @Test
    void dragsWithoutMutatingInitialBounds() {
        Rectangle initial = new Rectangle(100, 200, 800, 600);

        Rectangle dragged = WindowGeometry.draggedBounds(
                initial,
                new Point(140, 215),
                new Point(90, 300)
        );

        assertEquals(new Rectangle(50, 285, 800, 600), dragged);
        assertEquals(new Rectangle(100, 200, 800, 600), initial);
    }

    @Test
    void restoresMaximizedWindowUnderTheSameHorizontalGrabRatio() {
        Rectangle restored = WindowGeometry.restoredBoundsForDrag(
                new Rectangle(80, 70, 1000, 700),
                new Rectangle(0, 0, 2000, 1100),
                new Point(1500, 18),
                new Point(1200, 100)
        );

        assertEquals(new Rectangle(450, 82, 1000, 700), restored);
    }

    @Test
    void resizesNorthEastAndSouthWestEdges() {
        Rectangle initial = new Rectangle(100, 100, 400, 300);
        Point press = new Point(0, 0);
        Dimension minimum = new Dimension(100, 80);

        assertEquals(
                new Rectangle(100, 80, 450, 320),
                WindowGeometry.resizedBounds(initial, ResizeEdge.NORTH_EAST, press, new Point(50, -20), minimum)
        );
        assertEquals(
                new Rectangle(50, 100, 450, 340),
                WindowGeometry.resizedBounds(initial, ResizeEdge.SOUTH_WEST, press, new Point(-50, 40), minimum)
        );
    }

    @Test
    void resizesEachCardinalEdgeIndependently() {
        Rectangle initial = new Rectangle(100, 100, 400, 300);
        Point press = new Point(0, 0);
        Point pointer = new Point(20, 20);
        Dimension minimum = new Dimension(100, 80);

        assertEquals(
                new Rectangle(100, 120, 400, 280),
                WindowGeometry.resizedBounds(initial, ResizeEdge.NORTH, press, pointer, minimum)
        );
        assertEquals(
                new Rectangle(100, 100, 420, 300),
                WindowGeometry.resizedBounds(initial, ResizeEdge.EAST, press, pointer, minimum)
        );
        assertEquals(
                new Rectangle(100, 100, 400, 320),
                WindowGeometry.resizedBounds(initial, ResizeEdge.SOUTH, press, pointer, minimum)
        );
        assertEquals(
                new Rectangle(120, 100, 380, 300),
                WindowGeometry.resizedBounds(initial, ResizeEdge.WEST, press, pointer, minimum)
        );
    }

    @Test
    void clampsNorthWestAndSouthEastResizeToMinimumSize() {
        Rectangle initial = new Rectangle(100, 100, 400, 300);
        Point press = new Point(0, 0);
        Dimension minimum = new Dimension(100, 80);

        assertEquals(
                new Rectangle(400, 320, 100, 80),
                WindowGeometry.resizedBounds(initial, ResizeEdge.NORTH_WEST, press, new Point(500, 500), minimum)
        );
        assertEquals(
                new Rectangle(100, 100, 100, 80),
                WindowGeometry.resizedBounds(initial, ResizeEdge.SOUTH_EAST, press, new Point(-500, -500), minimum)
        );
    }

    @Test
    void pointerTakesPriorityWhenSelectingScreen() {
        List<Rectangle> screens = List.of(
                new Rectangle(-1920, 0, 1920, 1080),
                new Rectangle(0, 0, 1920, 1080)
        );

        int selected = WindowGeometry.selectScreen(
                screens,
                new Point(-100, 100),
                new Rectangle(100, 100, 800, 600)
        );

        assertEquals(0, selected);
    }

    @Test
    void largestWindowIntersectionAndNearestScreenAreFallbacks() {
        List<Rectangle> screens = List.of(
                new Rectangle(-1920, 0, 1920, 1080),
                new Rectangle(0, 0, 1920, 1080)
        );

        assertEquals(1, WindowGeometry.selectScreen(
                screens,
                null,
                new Rectangle(-100, 100, 1000, 600)
        ));
        assertEquals(1, WindowGeometry.selectScreen(screens, new Point(4000, 300), null));
    }

    @Test
    void requiresAtLeastOneScreen() {
        assertThrows(
                IllegalArgumentException.class,
                () -> WindowGeometry.selectScreen(List.of(), new Point(), new Rectangle())
        );
    }
}
