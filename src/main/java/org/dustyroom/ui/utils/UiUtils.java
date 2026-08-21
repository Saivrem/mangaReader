package org.dustyroom.ui.utils;

import lombok.experimental.UtilityClass;
import org.dustyroom.ui.LookSettings;
import org.dustyroom.ui.theme.ThemeManager;

import javax.swing.SwingUtilities;
import java.awt.Component;

@UtilityClass
public class UiUtils {

    public static LookSettings getCurrent() {
        return ThemeManager.current();
    }

    public static void redrawComponent(Component component) {
        SwingUtilities.updateComponentTreeUI(component);
    }
}
