package org.dustyroom;

import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipIterator;
import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import java.io.File;

import static org.dustyroom.be.utils.FileUtils.isSupported;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageViewer imageViewer = new ImageViewer();
            if (args.length > 0) {
                ImageIterator imageIterator = setupIterator(args);
                if (imageIterator != null) {
                    imageViewer.setImageIterator(imageIterator);
                    imageViewer.processPicture(imageIterator.next());
                }
            }
        });
    }

    private static ImageIterator setupIterator(String[] args) {
        ImageIterator imageIterator = null;
        File file = new File(String.join(" ", args));
        if (file.isFile()) {
            String name = file.getName();
            if (name.endsWith("zip")) {
                imageIterator = new ZipIterator(file);
            } else if (isSupported(name)) {
                imageIterator = new FileImageIterator(file);
            }
        }
        return imageIterator;
    }
}