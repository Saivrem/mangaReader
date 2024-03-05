package org.dustyroom;

import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipImageIterator;
import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageViewer imageViewer = new ImageViewer();
            ImageIterator imageIterator = null;
            if (args.length != 0) {
                File file = new File(String.join(" ", args));
                if (file.isFile()) {
                    if (file.getName().endsWith("zip")) {
                        imageIterator = new ZipImageIterator(file);
                    }
                }
            }
            if (imageIterator != null) {
                imageViewer.setImageIterator(imageIterator);
                imageViewer.processPicture(imageIterator.next());
            }
        });
    }
}