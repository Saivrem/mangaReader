package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class TestViewerActions implements ViewerActions {
    private final Map<String, Integer> invocations = new HashMap<>();

    private final Action openFile = action("openFile", "Open File", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), false);
    private final Action exit = action("exit", "Exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), false);
    private final Action escapeFullscreen = action("escapeFullscreen", "Exit Fullscreen", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), false);
    private final Action fitHeight = action("fitHeight", "Fit Height");
    private final Action fitWidth = action("fitWidth", "Fit Width");
    private final Action fitScreen = action("fitScreen", "Fit Screen");
    private final Action zoomIn = action("zoomIn", "Zoom In");
    private final Action zoomOut = action("zoomOut", "Zoom Out");
    private final Action nextImage = action("nextImage", "Next Page");
    private final Action prevImage = action("previousImage", "Previous Page");
    private final Action firstImage = action("firstImage", "First Page");
    private final Action lastImage = action("lastImage", "Last Page");
    private final Action nextVolume = action("nextVolume", "Next Volume");
    private final Action prevVolume = action("previousVolume", "Previous Volume");
    private final Action toggleFullscreen = action("toggleFullscreen", "Fullscreen", null, false);
    private final Action toggleTwoPageMode = action("toggleTwoPageMode", "Two-page Mode", null, false);
    private final Action setComicsReadingMode = action("setComicsReadingMode", "Comics (Left to Right)", null, false);
    private final Action setMangaReadingMode = action("setMangaReadingMode", "Manga (Right to Left)", null, true);
    private final Action setDarkTheme = action("setDarkTheme", "Dark", null, true);
    private final Action setLightTheme = action("setLightTheme", "Light", null, false);
    private final Action showAbout = action("showAbout", "About Manga Reader");

    int invocationCount(String id) {
        return invocations.getOrDefault(id, 0);
    }

    @Override public Action openFile() { return openFile; }
    @Override public Action exit() { return exit; }
    @Override public Action escapeFullscreen() { return escapeFullscreen; }
    @Override public Action fitHeight() { return fitHeight; }
    @Override public Action fitWidth() { return fitWidth; }
    @Override public Action fitScreen() { return fitScreen; }
    @Override public Action zoomIn() { return zoomIn; }
    @Override public Action zoomOut() { return zoomOut; }
    @Override public Action nextImage() { return nextImage; }
    @Override public Action prevImage() { return prevImage; }
    @Override public Action firstImage() { return firstImage; }
    @Override public Action lastImage() { return lastImage; }
    @Override public Action nextVolume() { return nextVolume; }
    @Override public Action prevVolume() { return prevVolume; }
    @Override public Action toggleFullscreen() { return toggleFullscreen; }
    @Override public Action toggleTwoPageMode() { return toggleTwoPageMode; }
    @Override public Action setComicsReadingMode() { return setComicsReadingMode; }
    @Override public Action setMangaReadingMode() { return setMangaReadingMode; }
    @Override public Action setDarkTheme() { return setDarkTheme; }
    @Override public Action setLightTheme() { return setLightTheme; }
    @Override public Action showAbout() { return showAbout; }

    private Action action(String id, String name) {
        return action(id, name, null, null);
    }

    private Action action(String id, String name, KeyStroke accelerator, Boolean selected) {
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
                invocations.merge(id, 1, Integer::sum);
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, id);
        action.putValue(Action.SHORT_DESCRIPTION, name + " description");
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        if (selected != null) {
            action.putValue(Action.SELECTED_KEY, selected);
        }
        return action;
    }
}
