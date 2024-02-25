package org.dustyroom;

import org.dustyroom.be.filewalking.FileTreeIterator;
import org.dustyroom.be.filewalking.MangaFileVisitor;
import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.dustyroom.be.utils.Constants.DARK_THEME;
import static org.dustyroom.be.utils.UiUtils.setDarkTheme;
import static org.dustyroom.be.utils.UiUtils.setSystemTheme;

public class Main {

    private static FileTreeIterator fileTreeIterator;

    public static void main(String[] args) throws Exception {
        Map<String, String> stringStringMap = readArgs(args);
        if (!stringStringMap.isEmpty()) {
            setupLookAndFeel(stringStringMap.get("-t"));
            fileTreeIterator = setupFileLocation(stringStringMap.get("-d"));
        } else {
            setSystemTheme();
        }

        SwingUtilities.invokeLater(() -> new ImageViewer(fileTreeIterator).setVisible(true));
    }

    private static void setupLookAndFeel(String param) {
        if (param != null) {
            if (param.equals(DARK_THEME)) {
                setDarkTheme();
            }
        }
    }

    private static FileTreeIterator setupFileLocation(String param) throws Exception {
        if (param != null) {
            Path path = Path.of(param);
            MangaFileVisitor fileVisitor = new MangaFileVisitor();
            Files.walkFileTree(path, fileVisitor);
            return new FileTreeIterator(fileVisitor.getTree(), path);
        }
        return null;
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