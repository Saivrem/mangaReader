package org.dustyroom.ui;

import java.util.Locale;
import java.awt.Desktop;
import javax.swing.SwingUtilities;

/**
 * Configures UI behavior which must be decided before AWT is initialized.
 */
public final class PlatformUi {
    private static final String SCREEN_MENU_BAR_PROPERTY = "apple.laf.useScreenMenuBar";

    private PlatformUi() {
    }

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
        if (!usesScreenMenuBar() || !Desktop.isDesktopSupported()) return;
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

    static void openFiles(java.util.List<java.io.File> files, java.util.function.Consumer<java.io.File> open) {
        // The reader displays one book at a time. Finder may send a batch; open its first item.
        if (!files.isEmpty()) {
            java.io.File file = files.get(0);
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
