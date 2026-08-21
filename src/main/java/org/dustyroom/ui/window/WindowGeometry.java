package org.dustyroom.ui.window;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;

/**
 * Pure geometry used by the window controller. Methods never mutate arguments,
 * which keeps the platform-independent behavior suitable for headless tests.
 */
public final class WindowGeometry {
    private WindowGeometry() {
    }

    public static Rectangle usableBounds(Rectangle screenBounds, Insets screenInsets) {
        Objects.requireNonNull(screenBounds, "screenBounds");
        Objects.requireNonNull(screenInsets, "screenInsets");

        int width = Math.max(0, screenBounds.width - screenInsets.left - screenInsets.right);
        int height = Math.max(0, screenBounds.height - screenInsets.top - screenInsets.bottom);
        return new Rectangle(
                screenBounds.x + screenInsets.left,
                screenBounds.y + screenInsets.top,
                width,
                height
        );
    }

    public static Rectangle draggedBounds(Rectangle initialBounds, Point pressOnScreen, Point pointerOnScreen) {
        Objects.requireNonNull(initialBounds, "initialBounds");
        Objects.requireNonNull(pressOnScreen, "pressOnScreen");
        Objects.requireNonNull(pointerOnScreen, "pointerOnScreen");

        return new Rectangle(
                initialBounds.x + pointerOnScreen.x - pressOnScreen.x,
                initialBounds.y + pointerOnScreen.y - pressOnScreen.y,
                initialBounds.width,
                initialBounds.height
        );
    }

    /**
     * Restores a maximized window under the pointer while preserving the
     * horizontal grab ratio and the vertical title-bar grab offset.
     */
    public static Rectangle restoredBoundsForDrag(
            Rectangle restoreBounds,
            Rectangle maximizedBounds,
            Point pressOnScreen,
            Point pointerOnScreen
    ) {
        Objects.requireNonNull(restoreBounds, "restoreBounds");
        Objects.requireNonNull(maximizedBounds, "maximizedBounds");
        Objects.requireNonNull(pressOnScreen, "pressOnScreen");
        Objects.requireNonNull(pointerOnScreen, "pointerOnScreen");

        double ratio = maximizedBounds.width <= 0
                ? 0.5
                : (pressOnScreen.x - maximizedBounds.x) / (double) maximizedBounds.width;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int grabOffsetY = Math.max(0, pressOnScreen.y - maximizedBounds.y);

        return new Rectangle(
                pointerOnScreen.x - (int) Math.round(restoreBounds.width * ratio),
                pointerOnScreen.y - grabOffsetY,
                restoreBounds.width,
                restoreBounds.height
        );
    }

    public static Rectangle resizedBounds(
            Rectangle initialBounds,
            ResizeEdge edge,
            Point pressOnScreen,
            Point pointerOnScreen,
            Dimension minimumSize
    ) {
        Objects.requireNonNull(initialBounds, "initialBounds");
        Objects.requireNonNull(edge, "edge");
        Objects.requireNonNull(pressOnScreen, "pressOnScreen");
        Objects.requireNonNull(pointerOnScreen, "pointerOnScreen");
        Objects.requireNonNull(minimumSize, "minimumSize");

        int dx = pointerOnScreen.x - pressOnScreen.x;
        int dy = pointerOnScreen.y - pressOnScreen.y;
        int minWidth = Math.max(1, minimumSize.width);
        int minHeight = Math.max(1, minimumSize.height);

        int left = initialBounds.x;
        int top = initialBounds.y;
        int right = initialBounds.x + initialBounds.width;
        int bottom = initialBounds.y + initialBounds.height;

        if (movesWest(edge)) {
            left = Math.min(left + dx, right - minWidth);
        }
        if (movesEast(edge)) {
            right = Math.max(right + dx, left + minWidth);
        }
        if (movesNorth(edge)) {
            top = Math.min(top + dy, bottom - minHeight);
        }
        if (movesSouth(edge)) {
            bottom = Math.max(bottom + dy, top + minHeight);
        }

        return new Rectangle(left, top, right - left, bottom - top);
    }

    /**
     * Chooses a screen by pointer, then by largest window intersection, then by
     * nearest screen center. Bounds may use negative virtual-desktop coordinates.
     */
    public static int selectScreen(List<Rectangle> screens, Point pointerOnScreen, Rectangle windowBounds) {
        Objects.requireNonNull(screens, "screens");
        if (screens.isEmpty()) {
            throw new IllegalArgumentException("At least one screen is required");
        }

        if (pointerOnScreen != null) {
            for (int i = 0; i < screens.size(); i++) {
                if (screens.get(i).contains(pointerOnScreen)) {
                    return i;
                }
            }
        }

        if (windowBounds != null) {
            long largestArea = -1;
            int largestIndex = 0;
            for (int i = 0; i < screens.size(); i++) {
                Rectangle intersection = screens.get(i).intersection(windowBounds);
                long area = intersection.isEmpty()
                        ? 0
                        : (long) intersection.width * intersection.height;
                if (area > largestArea) {
                    largestArea = area;
                    largestIndex = i;
                }
            }
            if (largestArea > 0) {
                return largestIndex;
            }
        }

        Point reference = pointerOnScreen != null
                ? pointerOnScreen
                : center(windowBounds == null ? screens.get(0) : windowBounds);
        double nearestDistance = Double.POSITIVE_INFINITY;
        int nearestIndex = 0;
        for (int i = 0; i < screens.size(); i++) {
            Point screenCenter = center(screens.get(i));
            double dx = screenCenter.x - reference.x;
            double dy = screenCenter.y - reference.y;
            double distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    public static Point center(Rectangle bounds) {
        Objects.requireNonNull(bounds, "bounds");
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    private static boolean movesWest(ResizeEdge edge) {
        return edge == ResizeEdge.WEST
                || edge == ResizeEdge.NORTH_WEST
                || edge == ResizeEdge.SOUTH_WEST;
    }

    private static boolean movesEast(ResizeEdge edge) {
        return edge == ResizeEdge.EAST
                || edge == ResizeEdge.NORTH_EAST
                || edge == ResizeEdge.SOUTH_EAST;
    }

    private static boolean movesNorth(ResizeEdge edge) {
        return edge == ResizeEdge.NORTH
                || edge == ResizeEdge.NORTH_EAST
                || edge == ResizeEdge.NORTH_WEST;
    }

    private static boolean movesSouth(ResizeEdge edge) {
        return edge == ResizeEdge.SOUTH
                || edge == ResizeEdge.SOUTH_EAST
                || edge == ResizeEdge.SOUTH_WEST;
    }
}
