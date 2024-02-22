package org.dustyroom;

import org.dustyroom.ui.ImageViewer;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new NimbusLookAndFeel() {
            @Override
            public UIDefaults getDefaults() {
                UIDefaults defaults = super.getDefaults();

                defaults.put("nimbusBase", new ColorUIResource(18, 30, 49));
                defaults.put("nimbusBlueGrey", new ColorUIResource(50, 57, 69));
                defaults.put("control", new ColorUIResource(50, 57, 69));
                defaults.put("nimbusLightBackground", new ColorUIResource(37, 51, 67));
                defaults.put("nimbusTitleBackground", new Color(18, 30, 49));
                defaults.put("nimbusTitleText", new Color(255, 255, 255));
                return defaults;
            }
        });
        MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        SwingUtilities.invokeLater(() -> new ImageViewer().setVisible(true));
    }
}