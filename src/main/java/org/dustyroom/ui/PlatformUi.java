package org.dustyroom.ui;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;

/**
 * Configures UI behavior which must be decided before AWT is initialized.
 */
@UtilityClass
public class PlatformUi {
    private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";

    public static void configure() {
        boolean macOs = isMacOs(System.getProperty("os.name", ""));

        // Be explicit on every platform. This also neutralizes an inherited
        // -Dapple.laf.useScreenMenuBar=true when the application runs on Windows.
        System.setProperty(SCREEN_MENU_BAR_PROPERTY, Boolean.toString(macOs));
        if (macOs) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", "Yonaka Reader");
        }
    }

    public static void installApplicationHandlers(ViewerController controller) {
        if (!usesScreenMenuBar() || !Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
            desktop.setOpenFileHandler(event -> openFiles(event.getFiles(), controller::openExternalFile));
        }
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(event -> SwingUtilities.invokeLater(controller::showAboutDialog));
        }
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((event, response) -> {
                response.cancelQuit();
                SwingUtilities.invokeLater(controller::exit);
            });
        }
    }

    static void openFiles(List<File> files, Consumer<File> open) {
        // The reader displays one book at a time. Finder may send a batch; open its first item.
        if (!files.isEmpty()) {
            File file = files.get(0);
            SwingUtilities.invokeLater(() -> open.accept(file));
        }
    }

    public static boolean usesScreenMenuBar() {
        return isMacOs(System.getProperty("os.name", ""));
    }

    static boolean isMacOs(String osName) {
        String normalizedName = osName.toLowerCase(Locale.ROOT);
        return normalizedName.startsWith("mac") || normalizedName.startsWith("darwin");
    }
}
