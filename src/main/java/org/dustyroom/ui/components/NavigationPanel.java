package org.dustyroom.ui.components;

import org.dustyroom.ui.components.listeners.CustomListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import static org.dustyroom.ui.utils.UiUtils.performAction;

public class NavigationPanel extends JPanel implements ActionListener {
    private final Map<JButton, CustomListener> listenerMap = new HashMap<>();

    public NavigationPanel init(
            CustomListener nextFileListener,
            CustomListener prevFileListener,
            CustomListener firstFileListener,
            CustomListener lastFileListener,
            CustomListener openFileListener,
            CustomListener nextVolumeListener,
            CustomListener prevVolumeListener
    ) {
        setLayout(new FlowLayout());

        // @formatter:off
        addButton("\uD83D\uDCC2⇤"         , prevVolumeListener);
        addButton("⇤"                     , firstFileListener);
        addButton("←"                     , prevFileListener);
        addButton("\uD83D\uDCC2"          , openFileListener);
        addButton("→"                     , nextFileListener);
        addButton("⇥"                     , lastFileListener);
        addButton("⇥\uD83D\uDCC2"         , nextVolumeListener);
        // @formatter: on

        return this;
    }

    private void addButton(String text, CustomListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(this);
        listenerMap.put(button, listener);
        add(button);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction(e, this, listenerMap);
    }
}
