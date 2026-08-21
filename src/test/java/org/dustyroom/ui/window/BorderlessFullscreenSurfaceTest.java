package org.dustyroom.ui.window;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderlessFullscreenSurfaceTest {

    @Test
    void targetsPhysicalMonitorBoundsRatherThanUsableWorkArea() {
        ScreenBoundsProvider.ScreenBounds screen = new ScreenBoundsProvider.ScreenBounds(
                null,
                new Rectangle(-1920, 0, 1920, 1080),
                new Rectangle(-1920, 24, 1920, 1016)
        );

        Rectangle target = WindowStateController.fullscreenTargetBounds(screen);

        assertEquals(new Rectangle(-1920, 0, 1920, 1080), target);
        target.setBounds(0, 0, 1, 1);
        assertEquals(new Rectangle(-1920, 0, 1920, 1080), screen.bounds());
    }

    @Test
    void entersAndExitsWithoutReplacingTheWindowSurface() {
        FakeFullscreenHost host = new FakeFullscreenHost(true);
        WindowStateController.BorderlessFullscreenSurface surface =
                new WindowStateController.BorderlessFullscreenSurface(host);
        Rectangle physicalMonitor = new Rectangle(-1600, 0, 1600, 900);

        surface.enter(physicalMonitor);

        assertFalse(host.resizable);
        assertTrue(host.chromeFullscreen);
        assertEquals(physicalMonitor, host.bounds);
        assertEquals(1, host.refreshCount);
        assertEquals(1, host.focusCount);
        assertEquals(List.of("resizable:false", "fullscreen:true", "bounds"), host.surfaceMutations);

        Rectangle maximizedBounds = new Rectangle(-1600, 24, 1600, 836);
        surface.exit(WindowMode.MAXIMIZED, maximizedBounds);

        assertTrue(host.resizable);
        assertFalse(host.chromeFullscreen);
        assertTrue(host.chromeMaximized);
        assertEquals(maximizedBounds, host.bounds);
        assertEquals(2, host.refreshCount);
        assertEquals(2, host.focusCount);
        assertEquals(List.of(
                "resizable:false", "fullscreen:true", "bounds",
                "fullscreen:false", "maximized:true", "resizable:true", "bounds"
        ), host.surfaceMutations);
    }

    @Test
    void restoresOriginallyNonResizableWindowAndRejectsInvalidLifecycle() {
        FakeFullscreenHost host = new FakeFullscreenHost(false);
        WindowStateController.BorderlessFullscreenSurface surface =
                new WindowStateController.BorderlessFullscreenSurface(host);

        assertThrows(
                IllegalStateException.class,
                () -> surface.exit(WindowMode.NORMAL, new Rectangle(20, 30, 800, 600))
        );

        surface.enter(new Rectangle(0, 0, 1920, 1080));
        assertThrows(
                IllegalStateException.class,
                () -> surface.enter(new Rectangle(0, 0, 1920, 1080))
        );
        surface.exit(WindowMode.NORMAL, new Rectangle(20, 30, 800, 600));

        assertFalse(host.resizable);
        assertFalse(host.chromeFullscreen);
        assertFalse(host.chromeMaximized);
        assertEquals(new Rectangle(20, 30, 800, 600), host.bounds);
    }

    private static final class FakeFullscreenHost implements WindowStateController.FullscreenHost {
        private final List<String> surfaceMutations = new ArrayList<>();
        private boolean resizable;
        private boolean chromeFullscreen;
        private boolean chromeMaximized;
        private Rectangle bounds;
        private int refreshCount;
        private int focusCount;

        private FakeFullscreenHost(boolean resizable) {
            this.resizable = resizable;
        }

        @Override
        public boolean isResizable() {
            return resizable;
        }

        @Override
        public void setResizable(boolean resizable) {
            this.resizable = resizable;
            surfaceMutations.add("resizable:" + resizable);
        }

        @Override
        public void setChromeFullscreen(boolean fullscreen) {
            chromeFullscreen = fullscreen;
            surfaceMutations.add("fullscreen:" + fullscreen);
        }

        @Override
        public void setChromeMaximized(boolean maximized) {
            chromeMaximized = maximized;
            surfaceMutations.add("maximized:" + maximized);
        }

        @Override
        public void setBounds(Rectangle bounds) {
            this.bounds = new Rectangle(bounds);
            surfaceMutations.add("bounds");
        }

        @Override
        public void refresh() {
            refreshCount++;
        }

        @Override
        public void requestFocus() {
            focusCount++;
        }
    }
}
