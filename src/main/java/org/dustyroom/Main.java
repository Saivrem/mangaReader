package org.dustyroom;

import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static org.dustyroom.be.utils.UiUtils.setSystemTheme;
import static org.dustyroom.be.utils.UiUtils.setupLookAndFeel;

public class Main {

    public static void main(String[] args) {
        Map<String, String> stringStringMap = readArgs(args);
        if (!stringStringMap.isEmpty()) {
            setupLookAndFeel(stringStringMap.get("-t"));
        } else {
            setSystemTheme();
        }

        SwingUtilities.invokeLater(ImageViewer::new);
    }

    private static Map<String, String> readArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        if (args.length > 0 && args.length % 2 == 0) {
            for (int i = 0; i < args.length; i += 2) {
                params.put(args[i], args[i + 1]);
            }
        }
        return params;
    }
}