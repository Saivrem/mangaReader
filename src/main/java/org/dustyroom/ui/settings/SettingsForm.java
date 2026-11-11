package org.dustyroom.ui.settings;

import javax.swing.*;
import java.awt.*;

final class SettingsForm extends JPanel {
    private int nextRow;

    SettingsForm() {
        super(new GridBagLayout());
    }

    void row(Component component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = nextRow++;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 12, 0);
        add(component, constraints);
    }
}
