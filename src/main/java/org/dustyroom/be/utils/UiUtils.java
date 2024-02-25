package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.LookSettings;

import javax.swing.*;
import java.awt.*;

@Slf4j
@UtilityClass
public class UiUtils {

    public static void setDarkTheme() {
        try {
            UIManager.setLookAndFeel(LookSettings.NIMBUS.getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Can't set Nimbus theme due to {}", e.getMessage());
        }
    }

    public static void setSystemTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
}
