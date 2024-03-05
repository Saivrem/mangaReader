package org.dustyroom;

import org.dustyroom.ui.ImageViewer;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageViewer::new);
    }
}