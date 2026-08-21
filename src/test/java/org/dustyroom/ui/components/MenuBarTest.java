package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBarTest {

    @Test
    void exposesSemanticMenusAndActionAccelerators() throws Exception {
        Fixture fixture = fixture();
        MenuBar menuBar = fixture.menuBar;

        assertEquals("mainMenuBar", menuBar.getName());
        assertEquals("Main menu", menuBar.getAccessibleContext().getAccessibleName());
        assertEquals(5, menuBar.getMenuCount());
        assertMenu(menuBar.getMenu(0), "File", KeyEvent.VK_F);
        assertMenu(menuBar.getMenu(1), "View", KeyEvent.VK_V);
        assertMenu(menuBar.getMenu(2), "Navigation", KeyEvent.VK_N);
        assertMenu(menuBar.getMenu(3), "Options", KeyEvent.VK_O);
        assertMenu(menuBar.getMenu(4), "Help", KeyEvent.VK_H);

        JMenuItem open = findItem(menuBar, fixture.actions.openFile());
        assertEquals(fixture.actions.openFile().getValue(Action.ACCELERATOR_KEY), open.getAccelerator());
        assertEquals("menu.openFile", open.getName());
        assertEquals("Open File", open.getAccessibleContext().getAccessibleName());
        assertEquals("Open File description", open.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void usesCheckItemsForTogglesAndExclusiveRadioItemsForModesAndThemes() throws Exception {
        Fixture fixture = fixture();

        JCheckBoxMenuItem fullscreen = assertInstanceOf(
                JCheckBoxMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.toggleFullscreen())
        );
        JCheckBoxMenuItem twoPage = assertInstanceOf(
                JCheckBoxMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.toggleTwoPageMode())
        );

        JRadioButtonMenuItem manga = assertInstanceOf(
                JRadioButtonMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.setMangaReadingMode())
        );
        JRadioButtonMenuItem comics = assertInstanceOf(
                JRadioButtonMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.setComicsReadingMode())
        );
        JRadioButtonMenuItem darkTheme = assertInstanceOf(
                JRadioButtonMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.setDarkTheme())
        );
        JRadioButtonMenuItem lightTheme = assertInstanceOf(
                JRadioButtonMenuItem.class,
                findItem(fixture.menuBar, fixture.actions.setLightTheme())
        );

        assertTrue(manga.isSelected());
        assertFalse(comics.isSelected());
        assertTrue(darkTheme.isSelected());
        assertFalse(lightTheme.isSelected());
        assertSame(darkTheme.getParent(), lightTheme.getParent());
        assertEquals(2, darkTheme.getParent().getComponentCount());

        SwingUtilities.invokeAndWait(() -> {
            fullscreen.doClick(0);
            twoPage.doClick(0);
        });
        assertTrue(fullscreen.isSelected());
        assertTrue(twoPage.isSelected());

        SwingUtilities.invokeAndWait(comics::doClick);
        assertTrue(comics.isSelected());
        assertFalse(manga.isSelected());
        assertEquals(1, fixture.actions.invocationCount("setComicsReadingMode"));

        SwingUtilities.invokeAndWait(lightTheme::doClick);
        assertTrue(lightTheme.isSelected());
        assertFalse(darkTheme.isSelected());
        assertEquals(1, fixture.actions.invocationCount("setLightTheme"));
    }

    private static Fixture fixture() throws Exception {
        return onEdt(() -> {
            TestViewerActions actions = new TestViewerActions();
            return new Fixture(new MenuBar(actions), actions);
        });
    }

    private static void assertMenu(JMenu menu, String text, int mnemonic) {
        assertNotNull(menu);
        assertEquals(text, menu.getText());
        assertEquals(mnemonic, menu.getMnemonic());
        assertEquals(text, menu.getAccessibleContext().getAccessibleName());
        assertEquals(text + " menu", menu.getAccessibleContext().getAccessibleDescription());
    }

    private static JMenuItem findItem(MenuBar menuBar, Action action) {
        for (int index = 0; index < menuBar.getMenuCount(); index++) {
            JMenuItem result = findItem(menuBar.getMenu(index), action);
            if (result != null) {
                return result;
            }
        }
        throw new AssertionError("Action not found in menu: " + action.getValue(Action.ACTION_COMMAND_KEY));
    }

    private static JMenuItem findItem(JMenu menu, Action action) {
        if (menu.getAction() == action) {
            return menu;
        }
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenu childMenu) {
                JMenuItem result = findItem(childMenu, action);
                if (result != null) {
                    return result;
                }
            } else if (component instanceof JMenuItem item && item.getAction() == action) {
                return item;
            }
        }
        return null;
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }

    private record Fixture(MenuBar menuBar, TestViewerActions actions) {
    }
}
