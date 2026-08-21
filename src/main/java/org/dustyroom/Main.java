package org.dustyroom;

import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        configurePlatformUi();
        SwingUtilities.invokeLater(() -> {
            ImageViewer imageViewer = new ImageViewer();
            if (args.length > 0) {
                imageViewer.openFile(new File(String.join(" ", args)));
            }
        });
    }

    private static void configurePlatformUi() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "Manga Reader");
        }
    }

}
