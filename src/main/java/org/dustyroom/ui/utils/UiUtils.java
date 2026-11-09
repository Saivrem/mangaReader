package org.dustyroom.ui.utils;

import com.formdev.flatlaf.FlatPropertiesLaf;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.theme.ViewerTheme;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@UtilityClass
public class UiUtils {
    private static ViewerTheme currentTheme = ViewerTheme.DARK;

    public static ViewerTheme currentTheme() {
        return currentTheme;
    }

    public static void installApplicationLookAndFeel() {
        installTheme(ViewerTheme.DARK);
    }

    public static boolean installTheme(ViewerTheme theme) {
        String path = "/org/dustyroom/ui/theme/" + theme.resource();
        try (InputStream stream = UiUtils.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing theme: " + path);
            Properties properties = new Properties();
            properties.load(stream);
            UIManager.setLookAndFeel(new FlatPropertiesLaf("Yonaka Reader " + theme.label(), properties));
            // Preserve OS window controls, system menus and window-management behavior.
            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
            currentTheme = theme;
            return true;
        } catch (IOException | UnsupportedLookAndFeelException exception) {
            log.error("Can't install application theme {}", theme, exception);
            return false;
        }
    }
}
