package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.*;
import java.awt.*;

public class NavigationPanel extends JPanel {
    public NavigationPanel (ViewerActions actions) {
        setLayout(new FlowLayout());

        // @formatter:off
        addButton("\uD83D\uDCC2⇤"         , actions.prevVolume());
        addButton("⇤"                     , actions.firstImage());
        addButton("←"                     , actions.prevImage());
        addButton("\uD83D\uDCC2"          , actions.openFile());
        addButton("→"                     , actions.nextImage());
        addButton("⇥"                     , actions.lastImage());
        addButton("⇥\uD83D\uDCC2"         , actions.nextVolume());
        // @formatter: on
    }

    private void addButton(String text, Action action) {
        JButton button = new JButton(action);
        button.setText(text);
        add(button);
    }
}
