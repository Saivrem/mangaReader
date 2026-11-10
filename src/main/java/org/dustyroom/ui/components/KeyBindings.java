package org.dustyroom.ui.components;

import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.dustyroom.ui.utils.PlatformUi;
import org.dustyroom.ui.actions.ViewerActions;

public class KeyBindings {

    private static void bindPrimary(InputMap inputMap, ActionMap actionMap, Action action) {
        Object accelerator = action.getValue(Action.ACCELERATOR_KEY);
        Object id = action.getValue(Action.ACTION_COMMAND_KEY);
        if (accelerator instanceof KeyStroke keyStroke && id instanceof String name) {
            bind(inputMap, actionMap, name, keyStroke, action);
        }
    }

    private static void bind(InputMap inputMap, ActionMap actionMap, String name,
                             KeyStroke keyStroke, Action action) {
        inputMap.put(keyStroke, name);
        actionMap.put(name, action);
    }

    private static KeyStroke key(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, 0);
    }

    public void init(JRootPane rootPane, ViewerActions actions) {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        bindPrimary(inputMap, actionMap, actions.toggleThumbnails());
        bindPrimary(inputMap, actionMap, actions.openFile());
        bindPrimary(inputMap, actionMap, actions.settings());
        bindPrimary(inputMap, actionMap, actions.home());
        bindPrimary(inputMap, actionMap, actions.goToPage());
        bindPrimary(inputMap, actionMap, actions.exitFullscreen());
        bindPrimary(inputMap, actionMap, actions.exit());
        bindPrimary(inputMap, actionMap, actions.fitHeight());
        bindPrimary(inputMap, actionMap, actions.fitWidth());
        bindPrimary(inputMap, actionMap, actions.fitScreen());
        bindPrimary(inputMap, actionMap, actions.zoomIn());
        bindPrimary(inputMap, actionMap, actions.zoomOut());
        bindPrimary(inputMap, actionMap, actions.nextImage());
        bindPrimary(inputMap, actionMap, actions.prevImage());
        bindPrimary(inputMap, actionMap, actions.firstImage());
        bindPrimary(inputMap, actionMap, actions.lastImage());
        bindPrimary(inputMap, actionMap, actions.nextVolume());
        bindPrimary(inputMap, actionMap, actions.prevVolume());
        bindPrimary(inputMap, actionMap, actions.toggleFullscreen());
        bindPrimary(inputMap, actionMap, actions.toggleTwoPageMode());
        bindPrimary(inputMap, actionMap, actions.setMangaReadingMode());
        bindPrimary(inputMap, actionMap, actions.setComicsReadingMode());

        if (!PlatformUi.usesScreenMenuBar()) {
            bind(inputMap, actionMap, "fullscreenF11", key(KeyEvent.VK_F11), actions.toggleFullscreen());
        }
        bind(inputMap, actionMap, "openFileLegacy", key(KeyEvent.VK_O), actions.openFile());
        bind(inputMap, actionMap, "exitLegacy", key(KeyEvent.VK_Q), actions.exit());
        bind(inputMap, actionMap, "nextImagePage", key(KeyEvent.VK_PAGE_DOWN), actions.nextImage());
        bind(inputMap, actionMap, "previousImagePage", key(KeyEvent.VK_PAGE_UP), actions.prevImage());
        bind(inputMap, actionMap, "zoomInEquals", key(KeyEvent.VK_EQUALS), actions.zoomIn());
    }
}
