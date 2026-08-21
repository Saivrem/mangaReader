package org.dustyroom;

import org.dustyroom.ui.ImageViewer;
import org.dustyroom.ui.theme.ThemeManager;
import org.dustyroom.ui.window.platform.DesktopPlatform;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        configurePlatformUi();
        ThemeManager.installDefault();
        SwingUtilities.invokeLater(() -> {
            ImageViewer imageViewer = new ImageViewer();
            imageViewer.showWindow();
            if (args.length > 0) {
                imageViewer.openFile(new File(String.join(" ", args)));
            }
        });
    }

    private static void configurePlatformUi() {
        if (DesktopPlatform.current() == DesktopPlatform.MACOS) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", "Manga Reader");
        }
    }

}
