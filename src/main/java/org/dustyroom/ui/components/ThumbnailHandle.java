package org.dustyroom.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.UIManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ThumbnailHandle extends JButton {

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = getModel().isRollover() || isFocusOwner();
            Color color = UIManager.getColor(active ? "Component.accentColor" : "Label.disabledForeground");
            g.setColor(color == null ? getForeground() : color);
            int width = active ? 5 : 3;
            g.fillRoundRect((getWidth() - width) / 2, (getHeight() - 32) / 2, width, 32, width, width);
        } finally {
            g.dispose();
        }
    }

}
