package org.dustyroom;

import org.dustyroom.ui.ImageViewer;
import org.dustyroom.ui.PlatformUi;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.SwingUtilities;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        PlatformUi.configure();
        SwingUtilities.invokeLater(() -> {
            UiUtils.installApplicationLookAndFeel();
            ImageViewer imageViewer = new ImageViewer();
            if (args.length > 0) {
                imageViewer.openFile(new File(String.join(" ", args)));
            }
        });
    }

}
