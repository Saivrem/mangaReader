package org.dustyroom.ui.actions;

import org.dustyroom.ui.LookSettings;
import org.dustyroom.ui.utils.UiUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultViewerActionsTest {
    private RecordingCommands commands;
    private DefaultViewerActions actions;

    @BeforeEach
    void setUp() {
        commands = new RecordingCommands();
        actions = new DefaultViewerActions(commands);
    }

    @Test
    void exposesSemanticMetadataForEveryAction() {
        for (Action action : allActions()) {
            assertNonBlank(action.getValue(Action.NAME));
            assertNonBlank(action.getValue(Action.ACTION_COMMAND_KEY));
            assertNonBlank(action.getValue(Action.SHORT_DESCRIPTION));
            assertEquals(action.getValue(Action.SHORT_DESCRIPTION), action.getValue(Action.LONG_DESCRIPTION));
            assertTrue(action.isEnabled());
        }

        assertInstanceOf(KeyStroke.class, actions.openFile().getValue(Action.ACCELERATOR_KEY));
        assertInstanceOf(KeyStroke.class, actions.exit().getValue(Action.ACCELERATOR_KEY));
        assertEquals(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                actions.escapeFullscreen().getValue(Action.ACCELERATOR_KEY)
        );
    }

    @Test
    void routesEveryActionToTheExpectedControllerCommand() {
        invoke(actions.openFile());
        invoke(actions.exit());
        invoke(actions.escapeFullscreen());
        invoke(actions.fitHeight());
        invoke(actions.fitWidth());
        invoke(actions.fitScreen());
        invoke(actions.zoomIn());
        invoke(actions.zoomOut());
        invoke(actions.nextImage());
        invoke(actions.prevImage());
        invoke(actions.firstImage());
        invoke(actions.lastImage());
        invoke(actions.nextVolume());
        invoke(actions.prevVolume());
        invoke(actions.toggleFullscreen());
        invoke(actions.toggleTwoPageMode());
        invoke(actions.setComicsReadingMode());
        invoke(actions.setMangaReadingMode());
        invoke(actions.setDarkTheme());
        invoke(actions.setLightTheme());
        invoke(actions.showAbout());

        assertEquals(List.of(
                "openFile", "exit", "escapeFullscreen", "fitHeight", "fitWidth", "fitScreen",
                "zoomIn", "zoomOut", "nextImage", "previousImage", "firstImage", "lastImage",
                "nextVolume", "previousVolume", "toggleFullscreen", "toggleTwoPageMode",
                "comicsReadingMode", "mangaReadingMode", "darkTheme", "lightTheme", "about"
        ), commands.invocations);
    }

    @Test
    void keepsToggleAndExclusiveSelectionStateInSync() {
        assertFalse(selected(actions.toggleFullscreen()));
        invoke(actions.toggleFullscreen());
        assertTrue(selected(actions.toggleFullscreen()));
        invoke(actions.escapeFullscreen());
        assertFalse(selected(actions.toggleFullscreen()));

        invoke(actions.toggleFullscreen());
        assertTrue(selected(actions.toggleFullscreen()));
        invoke(actions.toggleFullscreen());
        assertFalse(selected(actions.toggleFullscreen()));

        assertFalse(selected(actions.toggleTwoPageMode()));
        invoke(actions.toggleTwoPageMode());
        assertTrue(selected(actions.toggleTwoPageMode()));

        assertTrue(selected(actions.setMangaReadingMode()));
        assertFalse(selected(actions.setComicsReadingMode()));
        invoke(actions.setComicsReadingMode());
        assertTrue(selected(actions.setComicsReadingMode()));
        assertFalse(selected(actions.setMangaReadingMode()));

        LookSettings initialTheme = UiUtils.getCurrent() == null
                ? LookSettings.MANGA_READER_DARK
                : UiUtils.getCurrent();
        assertEquals(initialTheme == LookSettings.MANGA_READER_DARK, selected(actions.setDarkTheme()));
        assertEquals(initialTheme == LookSettings.MANGA_READER_LIGHT, selected(actions.setLightTheme()));
        Action requestedTheme = selected(actions.setDarkTheme())
                ? actions.setLightTheme()
                : actions.setDarkTheme();
        Action previousTheme = requestedTheme == actions.setDarkTheme()
                ? actions.setLightTheme()
                : actions.setDarkTheme();
        invoke(requestedTheme);
        assertTrue(selected(requestedTheme));
        assertFalse(selected(previousTheme));
    }

    @Test
    void swingToggleItemsDoNotDoubleToggleActionSelection() throws Exception {
        JCheckBoxMenuItem fullscreen = new JCheckBoxMenuItem(actions.toggleFullscreen());
        JCheckBoxMenuItem twoPage = new JCheckBoxMenuItem(actions.toggleTwoPageMode());

        SwingUtilities.invokeAndWait(() -> {
            fullscreen.doClick(0);
            twoPage.doClick(0);
        });

        assertTrue(fullscreen.isSelected());
        assertTrue(selected(actions.toggleFullscreen()));
        assertTrue(twoPage.isSelected());
        assertTrue(selected(actions.toggleTwoPageMode()));
    }

    @Test
    void plainButtonInvocationStillTogglesActionSelection() throws Exception {
        JButton fullscreen = new JButton(actions.toggleFullscreen());

        SwingUtilities.invokeAndWait(() -> fullscreen.doClick(0));

        assertTrue(selected(actions.toggleFullscreen()));
    }

    @Test
    void failedThemeChangePreservesPreviousRadioSelection() throws Exception {
        List<Boolean> before = themeSelection();
        commands.themeChangeSucceeds = false;
        ButtonGroup group = new ButtonGroup();
        List<JRadioButtonMenuItem> items = themeActions().stream()
                .map(JRadioButtonMenuItem::new)
                .toList();
        items.forEach(group::add);
        JRadioButtonMenuItem differentTheme = items.stream()
                .filter(item -> !item.isSelected())
                .findFirst()
                .orElseThrow();

        SwingUtilities.invokeAndWait(() -> differentTheme.doClick(0));

        assertEquals(before, themeSelection());
        for (int index = 0; index < items.size(); index++) {
            assertEquals(before.get(index), items.get(index).isSelected());
        }
    }

    private List<Action> allActions() {
        return List.of(
                actions.openFile(), actions.exit(), actions.escapeFullscreen(),
                actions.fitHeight(), actions.fitWidth(), actions.fitScreen(),
                actions.zoomIn(), actions.zoomOut(), actions.nextImage(), actions.prevImage(),
                actions.firstImage(), actions.lastImage(), actions.nextVolume(), actions.prevVolume(),
                actions.toggleFullscreen(), actions.toggleTwoPageMode(),
                actions.setComicsReadingMode(), actions.setMangaReadingMode(),
                actions.setDarkTheme(), actions.setLightTheme(), actions.showAbout()
        );
    }

    private List<Boolean> themeSelection() {
        return themeActions().stream().map(DefaultViewerActionsTest::selected).toList();
    }

    private List<Action> themeActions() {
        return List.of(
                actions.setDarkTheme(), actions.setLightTheme()
        );
    }

    private static void invoke(Action action) {
        action.actionPerformed(new ActionEvent(action, ActionEvent.ACTION_PERFORMED, "test"));
    }

    private static boolean selected(Action action) {
        return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
    }

    private static void assertNonBlank(Object value) {
        assertNotNull(value);
        assertFalse(value.toString().isBlank());
    }

    private static final class RecordingCommands implements ViewerCommandPort {
        private final List<String> invocations = new ArrayList<>();
        private boolean themeChangeSucceeds = true;

        @Override public void chooseFile() { invocations.add("openFile"); }
        @Override public void exit() { invocations.add("exit"); }
        @Override public void escapeFullscreen() { invocations.add("escapeFullscreen"); }
        @Override public void fitHeight() { invocations.add("fitHeight"); }
        @Override public void fitWidth() { invocations.add("fitWidth"); }
        @Override public void fitScreen() { invocations.add("fitScreen"); }
        @Override public void zoomIn() { invocations.add("zoomIn"); }
        @Override public void zoomOut() { invocations.add("zoomOut"); }
        @Override public void showNextImage() { invocations.add("nextImage"); }
        @Override public void showPreviousImage() { invocations.add("previousImage"); }
        @Override public void showFirstImage() { invocations.add("firstImage"); }
        @Override public void showLastImage() { invocations.add("lastImage"); }
        @Override public void showNextVolume() { invocations.add("nextVolume"); }
        @Override public void showPrevVolume() { invocations.add("previousVolume"); }
        @Override public void toggleFullscreen() { invocations.add("toggleFullscreen"); }
        @Override public void toggleTwoPageMode() { invocations.add("toggleTwoPageMode"); }
        @Override public void setComicsReadingMode() { invocations.add("comicsReadingMode"); }
        @Override public void setMangaReadingMode() { invocations.add("mangaReadingMode"); }
        @Override public boolean setDarkTheme() { invocations.add("darkTheme"); return themeChangeSucceeds; }
        @Override public boolean setLightTheme() { invocations.add("lightTheme"); return themeChangeSucceeds; }
        @Override public void showAboutDialog() { invocations.add("about"); }
    }
}
