package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

import static org.dustyroom.be.utils.Constants.DARK_THEME;
import static org.dustyroom.be.utils.Constants.METAL_THEME;
import static org.dustyroom.ui.LookSettings.METAL;
import static org.dustyroom.ui.LookSettings.NIMBUS;

@Slf4j
@UtilityClass
public class UiUtils {

    public static void setupLookAndFeel(String param) {
        if (StringUtils.isNotBlank(param)) {
            switch (param) {
                case DARK_THEME:
                    setDarkTheme();
                    break;
                case METAL_THEME:
                    setMetalTheme();
                    break;
                default:
                    setSystemTheme();
                    break;
            }
        } else {
            UiUtils.setSystemTheme();
        }
    }

    public static void setDarkTheme() {
        try {
            UIManager.setLookAndFeel(NIMBUS.getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Can't set Nimbus theme due to {}", e.getMessage());
        }
    }

    public static void setMetalTheme() {
        try {
            UIManager.setLookAndFeel(METAL.getLookAndFeel());
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
