package org.dustyroom.ui.components;

import lombok.experimental.Accessors;
import org.dustyroom.ui.components.listeners.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Accessors(chain = true)
public class NavigationPanel extends JPanel implements ActionListener {
    private final JButton openButton = new JButton("\uD83D\uDCC2");
    private final JButton nextButton = new JButton("→");
    private final JButton prevButton = new JButton("←");
    private final JButton firstButton = new JButton("⇤");
    private final JButton lastButton = new JButton("⇥");

    private final NextFileListener nextFileListener;
    private final PrevFileListener prevFileListener;
    private final FirstFileListener firstFileListener;
    private final LastFileListener lastFileListener;
    private final OpenFileListener openFileListener;

    public NavigationPanel(
            NextFileListener nextFileListener,
            PrevFileListener prevFileListener,
            FirstFileListener firstFileListener,
            LastFileListener lastFileListener,
            OpenFileListener openFileListener
    ) {
        this.nextFileListener = nextFileListener;
        this.prevFileListener = prevFileListener;
        this.firstFileListener = firstFileListener;
        this.lastFileListener = lastFileListener;
        this.openFileListener = openFileListener;

        nextButton.addActionListener(this);
        prevButton.addActionListener(this);
        firstButton.addActionListener(this);
        lastButton.addActionListener(this);
        openButton.addActionListener(this);

        setLayout(new FlowLayout());
        add(firstButton);
        add(prevButton);
        add(openButton);
        add(nextButton);
        add(lastButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton clicked = (JButton) e.getSource();
        if (clicked == nextButton) {
            nextFileListener.next();
        } else if (clicked == prevButton) {
            prevFileListener.prev();
        } else if (clicked == firstButton) {
            firstFileListener.first();
        } else if (clicked == lastButton) {
            lastFileListener.last();
        } else if (clicked == openButton) {
            openFileListener.open();
        }
        SwingUtilities.getWindowAncestor(this).requestFocus();
    }
}
