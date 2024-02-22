package org.dustyroom;

import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class Main {

    // TODO Figure how to customize title bar
    private final static NimbusLookAndFeel nimbusLookAndFeel = new NimbusLookAndFeel() {
        @Override
        public UIDefaults getDefaults() {
            UIDefaults defaults = super.getDefaults();
            defaults.put("nimbusBase", new ColorUIResource(18, 30, 49));
            defaults.put("nimbusBlueGrey", new ColorUIResource(50, 57, 69));
            defaults.put("control", new ColorUIResource(50, 57, 69));
            defaults.put("nimbusLightBackground", new ColorUIResource(37, 51, 67));
            defaults.put("nimbusTitleBackground", new ColorUIResource(18, 30, 49));
            defaults.put("nimbusTitleText", new ColorUIResource(255, 255, 255));
            return defaults;
        }
    };

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(() -> new ImageViewer().setVisible(true));
    }
}