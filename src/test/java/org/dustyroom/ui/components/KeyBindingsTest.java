package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class KeyBindingsTest {

    @Test
    void bindsActionAcceleratorsAndSupportedAliases() {
        Map<String, Action> actionsByMethod = actionsByMethod();
        ViewerActions actions = proxy(actionsByMethod);
        JRootPane rootPane = new JRootPane();

        new KeyBindings().init(rootPane, actions);

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke openShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        assertEquals("openFile", inputMap.get(openShortcut));
        assertSame(actionsByMethod.get("openFile"), rootPane.getActionMap().get("openFile"));

        assertEquals("openFileLegacy", inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0)));
        assertSame(actionsByMethod.get("openFile"), rootPane.getActionMap().get("openFileLegacy"));
        assertEquals("nextImagePage", inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)));
        if (!org.dustyroom.ui.PlatformUi.usesScreenMenuBar()) {
            assertEquals("fullscreenF11", inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
        }
    }

    private static Map<String, Action> actionsByMethod() {
        Map<String, Action> actions = new HashMap<>();
        for (var method : ViewerActions.class.getMethods()) {
            String id = method.getName();
            Action action = new AbstractAction(id) {
                @Override
                public void actionPerformed(ActionEvent event) {
                }
            };
            action.putValue(Action.ACTION_COMMAND_KEY, id);
            if (id.equals("openFile")) {
                action.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
            } else if (id.equals("nextImage")) {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
            }
            actions.put(id, action);
        }
        return actions;
    }

    private static ViewerActions proxy(Map<String, Action> actions) {
        return (ViewerActions) Proxy.newProxyInstance(
                ViewerActions.class.getClassLoader(),
                new Class<?>[]{ViewerActions.class},
                (proxy, method, arguments) -> actions.get(method.getName()));
    }
}
