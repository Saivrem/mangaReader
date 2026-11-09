package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.theme.ViewerTheme;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MenuBarTest {

    private static Stream<String> allMenuLabels(MenuBar menuBar) {
        return IntStream.range(0, menuBar.getMenuCount())
                .mapToObj(menuBar::getMenu)
                .flatMap(menu -> Stream.concat(
                        Stream.of(menu.getText()),
                        Arrays.stream(menu.getMenuComponents())
                                .filter(JMenuItem.class::isInstance)
                                .map(JMenuItem.class::cast)
                                .map(JMenuItem::getText)));
    }

    @Test
    void offersExactlyTwoApplicationThemes() {
        NoOpViewerActions actions = new NoOpViewerActions();
        MenuBar menuBar = new MenuBar(actions, theme -> true);

        assertEquals(5, menuBar.getMenuCount());
        assertFalse(allMenuLabels(menuBar).anyMatch("Color scheme"::equals));
        assertFalse(allMenuLabels(menuBar).anyMatch(label -> label.endsWith(" theme")));

        JMenu themeMenu = (JMenu) menuBar.getMenu(3).getItem(2);
        assertEquals("Theme", themeMenu.getText());
        assertEquals(2, themeMenu.getItemCount());
        assertEquals("Ayu Dark", themeMenu.getItem(0).getText());
        assertEquals("Ayu Light", themeMenu.getItem(1).getText());

        JMenuItem openItem = menuBar.getMenu(0).getItem(0);
        assertSame(actions.openFile(), openItem.getAction());
        assertEquals("Test Action", openItem.getText());
        assertEquals(actions.openFile().getValue(Action.ACCELERATOR_KEY), openItem.getAccelerator());
    }

    @Test
    void themeSelectionIsExclusiveAndRestoredAfterFailure() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean accept = new AtomicBoolean(true);
            AtomicReference<ViewerTheme> requested =
                    new AtomicReference<>();
            MenuBar menuBar = new MenuBar(new NoOpViewerActions(), theme -> {
                requested.set(theme);
                return accept.get();
            });
            JMenu themes = (JMenu) menuBar.getMenu(3).getItem(2);
            JMenuItem dark = themes.getItem(0);
            JMenuItem light = themes.getItem(1);
            light.doClick(0);
            assertEquals(ViewerTheme.LIGHT, requested.get());
            Assertions.assertTrue(light.isSelected());
            assertFalse(dark.isSelected());
            accept.set(false);
            dark.doClick(0);
            Assertions.assertTrue(light.isSelected());
            assertFalse(dark.isSelected());
            accept.set(true);
            dark.doClick(0);
            Assertions.assertTrue(dark.isSelected());
            assertFalse(light.isSelected());
        });
    }

    private static final class NoOpViewerActions implements ViewerActions {
        private final Action action = new AbstractAction("Test Action") {
            @Override
            public void actionPerformed(ActionEvent event) {
            }
        };

        private NoOpViewerActions() {
            action.putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public Action toggleThumbnails() {
            return action;
        }

        @Override
        public Action home() {
            return action;
        }

        public Action openFile() {
            return action;
        }

        @Override
        public Action exit() {
            return action;
        }

        @Override
        public Action fitHeight() {
            return action;
        }

        @Override
        public Action fitWidth() {
            return action;
        }

        @Override
        public Action fitScreen() {
            return action;
        }

        @Override
        public Action zoomIn() {
            return action;
        }

        @Override
        public Action zoomOut() {
            return action;
        }

        @Override
        public Action nextImage() {
            return action;
        }

        @Override
        public Action prevImage() {
            return action;
        }

        @Override
        public Action firstImage() {
            return action;
        }

        @Override
        public Action lastImage() {
            return action;
        }

        @Override
        public Action nextVolume() {
            return action;
        }

        @Override
        public Action prevVolume() {
            return action;
        }

        @Override
        public Action toggleFullscreen() {
            return action;
        }

        @Override
        public Action toggleTwoPageMode() {
            return action;
        }

        @Override
        public Action setComicsReadingMode() {
            return action;
        }

        @Override
        public Action setMangaReadingMode() {
            return action;
        }

        @Override
        public Action goToPage() {
            return action;
        }

        @Override
        public Action exitFullscreen() {
            return action;
        }

        @Override
        public Action resetApplication() {
            return action;
        }

        @Override
        public Action showAbout() {
            return action;
        }
    }
}
