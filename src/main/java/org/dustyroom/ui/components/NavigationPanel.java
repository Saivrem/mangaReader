package org.dustyroom.ui.components;

import lombok.experimental.Accessors;
import org.dustyroom.ui.components.listeners.CustomListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

@Accessors(chain = true)
public class NavigationPanel extends JPanel implements ActionListener {
    private final Map<JButton, CustomListener> listenerMap = new HashMap<>();

    public NavigationPanel(
            CustomListener nextFileListener,
            CustomListener prevFileListener,
            CustomListener firstFileListener,
            CustomListener lastFileListener,
            CustomListener openFileListener,
            CustomListener nextVolumeListener,
            CustomListener prevVolumeListener
    ) {
        JButton openButton = new JButton("\uD83D\uDCC2");
        JButton nextButton = new JButton("→");
        JButton prevButton = new JButton("←");
        JButton firstButton = new JButton("⇤");
        JButton lastButton = new JButton("⇥");
        JButton nextVolumeButton = new JButton("⇥\uD83D\uDCC2");
        JButton prevVolumeButton = new JButton("\uD83D\uDCC2⇤");

        nextButton.addActionListener(this);
        prevButton.addActionListener(this);
        firstButton.addActionListener(this);
        lastButton.addActionListener(this);
        openButton.addActionListener(this);
        nextVolumeButton.addActionListener(this);
        prevVolumeButton.addActionListener(this);

        setLayout(new FlowLayout());
        add(prevVolumeButton);
        add(firstButton);
        add(prevButton);
        add(openButton);
        add(nextButton);
        add(lastButton);
        add(nextVolumeButton);

        listenerMap.put(nextButton, nextFileListener);
        listenerMap.put(prevButton, prevFileListener);
        listenerMap.put(firstButton, firstFileListener);
        listenerMap.put(lastButton, lastFileListener);
        listenerMap.put(openButton, openFileListener);
        listenerMap.put(prevVolumeButton, prevVolumeListener);
        listenerMap.put(nextVolumeButton, nextVolumeListener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton clicked = (JButton) e.getSource();
        listenerMap.keySet()
                .stream()
                .filter(k -> k == clicked)
                .findFirst()
                .ifPresent(pressed -> listenerMap.get(pressed).performAction());
        SwingUtilities.getWindowAncestor(this).requestFocus();
    }
}
