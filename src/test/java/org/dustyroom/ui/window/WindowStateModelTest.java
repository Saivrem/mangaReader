package org.dustyroom.ui.window;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowStateModelTest {
    @Test
    void maximizeAndRestorePreserveDefensiveNormalBounds() {
        WindowStateModel state = new WindowStateModel();
        Rectangle normal = new Rectangle(40, 50, 900, 700);

        state.maximize(normal);
        normal.setBounds(0, 0, 1, 1);

        assertEquals(WindowMode.MAXIMIZED, state.mode());
        assertEquals(new Rectangle(40, 50, 900, 700), state.restoreFromMaximized());
        assertEquals(WindowMode.NORMAL, state.mode());
    }

    @Test
    void fullscreenRestoresNormalModeAndBounds() {
        WindowStateModel state = new WindowStateModel();
        Rectangle normal = new Rectangle(-1200, 80, 1000, 720);
        Rectangle expected = new Rectangle(normal);

        state.enterFullscreen(normal);
        normal.setBounds(0, 0, 1, 1);
        WindowStateModel.FullscreenRestore restore = state.exitFullscreen();

        assertEquals(WindowMode.NORMAL, restore.mode());
        assertEquals(expected, restore.bounds());
        Rectangle returned = restore.bounds();
        returned.setBounds(0, 0, 2, 2);
        assertEquals(expected, restore.bounds());
        assertEquals(WindowMode.NORMAL, state.mode());
    }

    @Test
    void fullscreenRestoresMaximizedModeThenOriginalNormalBounds() {
        WindowStateModel state = new WindowStateModel();
        Rectangle normal = new Rectangle(60, 70, 800, 600);
        Rectangle maximized = new Rectangle(0, 24, 1920, 1016);

        state.maximize(normal);
        state.enterFullscreen(maximized);
        WindowStateModel.FullscreenRestore restore = state.exitFullscreen();

        assertEquals(WindowMode.MAXIMIZED, restore.mode());
        assertEquals(maximized, restore.bounds());
        assertEquals(normal, state.restoreFromMaximized());
    }

    @Test
    void rejectsInvalidStateTransitions() {
        WindowStateModel state = new WindowStateModel();

        assertThrows(IllegalStateException.class, state::restoreFromMaximized);
        state.enterFullscreen(new Rectangle(0, 0, 100, 100));
        assertThrows(
                IllegalStateException.class,
                () -> state.maximize(new Rectangle(10, 10, 50, 50))
        );
    }
}
