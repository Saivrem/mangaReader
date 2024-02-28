package org.dustyroom.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

@RequiredArgsConstructor
@Getter
public enum LookSettings {

    NIMBUS(new NimbusLookAndFeel() {
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
    }),
    METAL(new MetalLookAndFeel());

    private final LookAndFeel lookAndFeel;
}
