package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class NavigationPanel extends JPanel {
    public NavigationPanel(ViewerActions actions) {
        setLayout(new FlowLayout());
        setName("navigationPanel");
        getAccessibleContext().setAccessibleName("Page navigation");
        getAccessibleContext().setAccessibleDescription("Open files and navigate between pages and volumes");

        addButton(NavigationIconType.PREVIOUS_VOLUME, actions.prevVolume());
        addButton(NavigationIconType.FIRST_PAGE, actions.firstImage());
        addButton(NavigationIconType.PREVIOUS_PAGE, actions.prevImage());
        addButton(NavigationIconType.OPEN_FILE, actions.openFile());
        addButton(NavigationIconType.NEXT_PAGE, actions.nextImage());
        addButton(NavigationIconType.LAST_PAGE, actions.lastImage());
        addButton(NavigationIconType.NEXT_VOLUME, actions.nextVolume());
    }

    private void addButton(NavigationIconType iconType, Action action) {
        JButton button = new JButton(action);
        String name = stringValue(action, Action.NAME);
        String description = stringValue(action, Action.SHORT_DESCRIPTION);
        String id = stringValue(action, Action.ACTION_COMMAND_KEY);

        button.setHideActionText(true);
        button.setText(null);
        button.setIcon(new NavigationIcon(iconType));
        button.setToolTipText(description);
        button.setName("navigation." + id);
        button.getAccessibleContext().setAccessibleName(name);
        button.getAccessibleContext().setAccessibleDescription(description);
        add(button);
    }

    private String stringValue(Action action, String key) {
        Object value = action.getValue(key);
        return value == null ? "" : value.toString();
    }

    private enum NavigationIconType {
        PREVIOUS_VOLUME,
        FIRST_PAGE,
        PREVIOUS_PAGE,
        OPEN_FILE,
        NEXT_PAGE,
        LAST_PAGE,
        NEXT_VOLUME
    }

    private record NavigationIcon(NavigationIconType type) implements Icon {
        private static final int HEIGHT = 22;
        private static final int PAGE_ICON_WIDTH = 22;
        private static final int VOLUME_ICON_WIDTH = 36;

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                Color color = component.isEnabled()
                        ? component.getForeground()
                        : UIManager.getColor("Button.disabledText");
                g.setColor(color == null ? Color.GRAY : color);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);

                switch (type) {
                    case PREVIOUS_VOLUME -> {
                        drawArrow(g, 11, 3, 12);
                        drawFolder(g, 16);
                    }
                    case FIRST_PAGE -> {
                        g.drawLine(5, 5, 5, 17);
                        drawArrow(g, 16, 7, 11);
                    }
                    case PREVIOUS_PAGE -> drawArrow(g, 17, 6, 11);
                    case OPEN_FILE -> drawFolder(g);
                    case NEXT_PAGE -> drawArrow(g, 5, 16, 11);
                    case LAST_PAGE -> {
                        drawArrow(g, 6, 15, 11);
                        g.drawLine(17, 5, 17, 17);
                    }
                    case NEXT_VOLUME -> {
                        drawFolder(g, 3);
                        drawArrow(g, 24, 33, 12);
                    }
                }
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return type == NavigationIconType.PREVIOUS_VOLUME || type == NavigationIconType.NEXT_VOLUME
                    ? VOLUME_ICON_WIDTH
                    : PAGE_ICON_WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }

        private void drawFolder(Graphics2D g) {
            drawFolder(g, 3);
        }

        private void drawFolder(Graphics2D g, int left) {
            Path2D folder = new Path2D.Double();
            folder.moveTo(left, 7);
            folder.lineTo(left + 6, 7);
            folder.lineTo(left + 8, 9);
            folder.lineTo(left + 16, 9);
            folder.lineTo(left + 16, 18);
            folder.lineTo(left, 18);
            folder.closePath();
            g.draw(folder);
        }

        private void drawArrow(Graphics2D g, int fromX, int toX, int centerY) {
            g.drawLine(fromX, centerY, toX, centerY);
            int direction = Integer.compare(toX, fromX);
            int arrowBaseX = toX - direction * 4;
            g.drawLine(toX, centerY, arrowBaseX, centerY - 4);
            g.drawLine(toX, centerY, arrowBaseX, centerY + 4);
        }
    }
}
