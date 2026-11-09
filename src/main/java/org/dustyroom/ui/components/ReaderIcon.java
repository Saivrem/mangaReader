package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Original vector icons, drawn in logical coordinates for HiDPI displays.
 */
public record ReaderIcon(ReaderIconKind kind, int size) implements Icon {
    public ReaderIcon(ReaderIconKind kind) {
        this(kind, 20);
    }

    private static void line(Graphics2D g, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        g.draw(path);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(size / 24.0, size / 24.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = component.isEnabled() ? component.getForeground() : UIManager.getColor("Label.disabledForeground");
            if (component.isEnabled() && component instanceof AbstractButton button && button.isSelected()) {
                color = UIManager.getColor("ToggleButton.selectedForeground");
            }
            g.setColor(color == null ? component.getForeground() : color);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case OPEN -> {
                    line(g, 3, 8, 3, 5, 9, 5, 12, 8, 21, 8, 21, 19, 3, 19, 3, 8);
                    line(g, 3, 11, 21, 11);
                }
                case PREVIOUS -> line(g, 15, 5, 8, 12, 15, 19);
                case NEXT -> line(g, 9, 5, 16, 12, 9, 19);
                case PREVIOUS_VOLUME -> {
                    line(g, 7, 5, 7, 19);
                    line(g, 17, 5, 10, 12, 17, 19);
                }
                case NEXT_VOLUME -> {
                    line(g, 17, 5, 17, 19);
                    line(g, 7, 5, 14, 12, 7, 19);
                }
                case SPREAD, BOOK -> {
                    line(g, 12, 6, 8, 4, 3, 4, 3, 19, 8, 19, 12, 21, 16, 19, 21, 19, 21, 4, 16, 4, 12, 6, 12, 21);
                    if (kind == ReaderIconKind.BOOK) {
                        line(g, 6, 9, 9, 10);
                        line(g, 15, 10, 18, 9);
                    }
                }
                case FULLSCREEN -> {
                    line(g, 9, 4, 4, 4, 4, 9);
                    line(g, 15, 4, 20, 4, 20, 9);
                    line(g, 4, 15, 4, 20, 9, 20);
                    line(g, 15, 20, 20, 20, 20, 15);
                }
            }
        } finally {
            g.dispose();
        }
    }


}
