package org.dustyroom.ui.utils;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.LookSettings;
import org.dustyroom.ui.components.listeners.CustomListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

import static org.dustyroom.ui.LookSettings.*;

@Slf4j
@UtilityClass
public class UiUtils {

    @Getter
    public static LookSettings current;

    public static void setDarkTheme() {
        try {
            UIManager.setLookAndFeel(NIMBUS.getLookAndFeel());
            current = NIMBUS;
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Can't set Nimbus theme due to {}", e.getMessage());
        }
    }

    public static void setMetalTheme() {
        try {
            UIManager.setLookAndFeel(METAL.getLookAndFeel());
            current = METAL;
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Can't set Metal theme due to {}", e.getMessage());
        }
    }

    public static void setSystemTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            current = SYSTEM;
        } catch (ClassNotFoundException |
                 InstantiationException |
                 IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            log.error("Can't set system theme due to {}", e.getMessage());
        }
    }

    public static void redrawComponent(Component component) {
        SwingUtilities.updateComponentTreeUI(component);
    }

    public static <T> void performAction(ActionEvent e, Component c, Map<T, CustomListener> listenerMap) {
        T clicked = (T) e.getSource();
        CustomListener customListener = listenerMap.get(clicked);
        if (customListener != null) customListener.performAction();
        SwingUtilities.getWindowAncestor(c).requestFocus();
    }
}
