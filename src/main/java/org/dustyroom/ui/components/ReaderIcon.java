package org.dustyroom.ui.components;

import lombok.Builder;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Original vector icons, drawn in logical coordinates for HiDPI displays.
 */
public record ReaderIcon(ReaderIconKind kind, int size) implements Icon {
    @Builder
    public ReaderIcon {
    }

    public ReaderIcon(ReaderIconKind kind) {
        this(kind, 20);
    }

    private static void line(Graphics2D g, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        g.draw(path);
    }

    private static void volume(Graphics2D graphics, boolean next) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            if (next) {
                copy.translate(24, 0);
                copy.scale(-1, 1);
            }
            copy.drawRoundRect(9, 4, 11, 16, 2, 2);
            line(copy, 12, 4, 12, 20);
            line(copy, 2, 12, 7, 12);
            line(copy, 5, 9, 2, 12, 5, 15);
        } finally {
            copy.dispose();
        }
    }

    private static Color foreground(Component component) {
        if (component == null) {
            return UIManager.getColor("MenuItem.foreground");
        }
        Color stateColor = null;
        if (!component.isEnabled()) {
            stateColor = UIManager.getColor("Label.disabledForeground");
        } else if (component instanceof AbstractButton button && button.isSelected()) {
            stateColor = UIManager.getColor("ToggleButton.selectedForeground");
        }
        return stateColor != null ? stateColor : component.getForeground();
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
            Color color = foreground(component);
            if (color != null) {
                g.setColor(color);
            }
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case HOME -> {
                    line(g, 3, 11, 12, 3, 21, 11);
                    line(g, 5, 10, 5, 21, 10, 21, 10, 15, 14, 15, 14, 21, 19, 21, 19, 10);
                }
                case OPEN -> {
                    line(g, 3, 8, 3, 5, 9, 5, 12, 8, 21, 8, 21, 19, 3, 19, 3, 8);
                    line(g, 3, 11, 21, 11);
                }
                case PREVIOUS -> line(g, 15, 5, 8, 12, 15, 19);
                case NEXT -> line(g, 9, 5, 16, 12, 9, 19);
                case FIRST -> {
                    line(g, 5, 5, 5, 19);
                    line(g, 17, 5, 10, 12, 17, 19);
                }
                case LAST -> {
                    line(g, 19, 5, 19, 19);
                    line(g, 7, 5, 14, 12, 7, 19);
                }
                case PREVIOUS_VOLUME -> volume(g, false);
                case NEXT_VOLUME -> volume(g, true);
                case SPREAD, BOOK -> {
                    line(g, 12, 6, 8, 4, 3, 4, 3, 19, 8, 19, 12, 21, 16, 19, 21, 19, 21, 4, 16, 4, 12, 6, 12, 21);
                    if (kind == ReaderIconKind.BOOK) {
                        line(g, 6, 9, 9, 10);
                        line(g, 15, 10, 18, 9);
                    }
                }
                case MENU -> {
                    line(g, 3, 6, 21, 6);
                    line(g, 3, 12, 21, 12);
                    line(g, 3, 18, 21, 18);
                }
                case PAGE_GRID -> {
                    for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 3; column++) {
                            g.fillRoundRect(3 + 7 * column, 3 + 7 * row, 5, 5, 1, 1);
                        }
                    }
                }
                case FULLSCREEN -> {
                    line(g, 9, 4, 4, 4, 4, 9);
                    line(g, 15, 4, 20, 4, 20, 9);
                    line(g, 4, 15, 4, 20, 9, 20);
                    line(g, 15, 20, 20, 20, 20, 15);
                }
                default -> throw new IllegalStateException("Unexpected value: " + kind);
            }
        } finally {
            g.dispose();
        }
    }

}
