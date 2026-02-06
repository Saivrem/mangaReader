package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class KeyBindings {

    public void init(JRootPane rootPane, ViewerActions actions) {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        bind(inputMap, actionMap, "nextImage", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), actions.nextImage());
        bind(inputMap, actionMap, "nextImagePg", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), actions.nextImage());

        bind(inputMap, actionMap, "prevImage", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), actions.prevImage());
        bind(inputMap, actionMap, "prevImagePg", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), actions.prevImage());

        bind(inputMap, actionMap, "firstImage", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), actions.firstImage());
        bind(inputMap, actionMap, "lastImage", KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), actions.lastImage());

        bind(inputMap, actionMap, "toggleFullscreen", KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), actions.toggleFullscreen());
        bind(inputMap, actionMap, "openFile", KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), actions.openFile());

        bind(inputMap, actionMap, "fitHeight", KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), actions.fitHeight());
        bind(inputMap, actionMap, "fitWidth", KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), actions.fitWidth());
        bind(inputMap, actionMap, "fitScreen", KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), actions.fitScreen());
        bind(inputMap, actionMap, "toggleTwoPage", KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), actions.toggleTwoPageMode());
        bind(inputMap, actionMap, "toggleManga", KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), actions.setMangaReadingMode());
        bind(inputMap, actionMap, "toggleComic", KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), actions.setComicsReadingMode());

        bind(inputMap, actionMap, "zoomInPlus", KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), actions.zoomIn());
        bind(inputMap, actionMap, "zoomInEquals", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), actions.zoomIn());
        bind(inputMap, actionMap, "zoomOut", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), actions.zoomOut());

        bind(inputMap, actionMap, "prevVolume", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), actions.prevVolume());
        bind(inputMap, actionMap, "nextVolume", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), actions.nextVolume());

        bind(inputMap, actionMap, "quitEsc", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actions.exit());
        bind(inputMap, actionMap, "quitQ", KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), actions.exit());
    }

    private void bind(InputMap inputMap, ActionMap actionMap, String name, KeyStroke keyStroke, Action action) {
        inputMap.put(keyStroke, name);
        actionMap.put(name, action);
    }
}
