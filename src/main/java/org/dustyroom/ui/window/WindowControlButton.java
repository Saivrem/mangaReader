package org.dustyroom.ui.window;

import org.dustyroom.ui.window.platform.ChromeSpec;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/** A platform-adaptive minimize, maximize/restore, or close title-bar button. */
public final class WindowControlButton extends JButton {
    private static final Dimension FALLBACK_SIZE = new Dimension(44, 30);
    private static final Dimension MACOS_FALLBACK_SIZE = new Dimension(20, 30);
    private static final int MINIMUM_TRAFFIC_LIGHT_DIAMETER = 8;

    private final Control control;
    private final ChromeSpec chromeSpec;
    private boolean windowActive = true;
    private boolean maximized;
    private boolean controlGroupHovered;

    public WindowControlButton(Control control, Runnable command) {
        this(control, command, ChromeSpec.neutral());
    }

    public WindowControlButton(Control control, Runnable command, ChromeSpec chromeSpec) {
        this.control = Objects.requireNonNull(control, "control");
        Objects.requireNonNull(command, "command");
        this.chromeSpec = Objects.requireNonNull(chromeSpec, "chromeSpec");

        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setFocusable(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        setRolloverEnabled(true);
        updateAccessibleText(defaultAccessibleName());
        addActionListener((ActionEvent event) -> command.run());
        getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "activateWindowControl"
        );
        getActionMap().put("activateWindowControl", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doClick(0);
            }
        });
    }

    public Control control() {
        return control;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void setMaximized(boolean maximized) {
        if (this.maximized == maximized) {
            return;
        }
        this.maximized = maximized;
        if (control == Control.MAXIMIZE) {
            String name = maximized ? "Restore" : defaultAccessibleName();
            updateAccessibleText(name);
            repaint();
        }
    }

    void setWindowActive(boolean active) {
        windowActive = active;
        repaint();
    }

    void setControlGroupHovered(boolean hovered) {
        if (controlGroupHovered == hovered) {
            return;
        }
        controlGroupHovered = hovered;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        boolean macos = chromeSpec.visualStyle() == ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS;
        String key = macos
                ? "MangaReader.macTitleBarButtonSize"
                : "MangaReader.titleBarButtonSize";
        Dimension configured = UIManager.getDimension(key);
        Dimension fallback = macos ? MACOS_FALLBACK_SIZE : FALLBACK_SIZE;
        return configured == null || configured.width <= 0 || configured.height <= 0
                ? new Dimension(fallback)
                : new Dimension(configured);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(preferred.width, Integer.MAX_VALUE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintButtonBackground(g2);
            paintSymbol(g2);
            paintFocusIndicator(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintButtonBackground(Graphics2D g2) {
        if (chromeSpec.visualStyle() == ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS) {
            paintTrafficLight(g2);
            return;
        }
        if (!getModel().isRollover() && !getModel().isPressed()) {
            return;
        }

        Color color;
        boolean windowsClose = control == Control.CLOSE
                && chromeSpec.visualStyle() == ChromeSpec.VisualStyle.WINDOWS;
        if (windowsClose) {
            color = getModel().isPressed()
                    ? uiColor("MangaReader.titleBarClosePressedBackground", new Color(184, 46, 46))
                    : uiColor("MangaReader.titleBarCloseHoverBackground", new Color(196, 43, 28));
        } else {
            Color fallback = standardButtonFeedback(getModel().isPressed());
            color = getModel().isPressed()
                    ? uiColor("MangaReader.titleBarButtonPressedBackground", fallback)
                    : uiColor("MangaReader.titleBarButtonHoverBackground", fallback);
        }
        g2.setColor(color);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void paintSymbol(Graphics2D g2) {
        if (chromeSpec.visualStyle() == ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS) {
            paintTrafficLightSymbol(g2);
            return;
        }
        Color foreground = uiColor(
                "MangaReader.titleBarForeground",
                uiColor("Label.foreground", new Color(233, 233, 233))
        );
        if (!windowActive) {
            Color fallbackBackground = getParent() == null ? Color.DARK_GRAY : getParent().getBackground();
            foreground = uiColor(
                    "MangaReader.titleBarInactiveForeground",
                    blend(foreground, fallbackBackground, 0.45f)
            );
        }
        if (control == Control.CLOSE
                && chromeSpec.visualStyle() == ChromeSpec.VisualStyle.WINDOWS
                && (getModel().isRollover() || getModel().isPressed())) {
            foreground = Color.WHITE;
        }
        if (!isEnabled()) {
            foreground = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 110);
        }

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int symbolSize = Math.max(8, Math.min(11, Math.min(getWidth(), getHeight()) / 3));
        int half = symbolSize / 2;

        g2.setColor(foreground);
        g2.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        switch (control) {
            case MINIMIZE -> g2.drawLine(cx - half, cy + 2, cx + half, cy + 2);
            case CLOSE -> {
                g2.drawLine(cx - half, cy - half, cx + half, cy + half);
                g2.drawLine(cx + half, cy - half, cx - half, cy + half);
            }
            case MAXIMIZE -> paintMaximizeSymbol(g2, cx, cy, symbolSize);
        }
    }

    private void paintMaximizeSymbol(Graphics2D g2, int cx, int cy, int symbolSize) {
        int half = symbolSize / 2;
        if (!maximized) {
            g2.drawRect(cx - half, cy - half, symbolSize, symbolSize);
            return;
        }

        int inset = 3;
        g2.drawRect(cx - half + inset, cy - half, symbolSize - inset, symbolSize - inset);
        g2.drawRect(cx - half, cy - half + inset, symbolSize - inset, symbolSize - inset);
    }

    private void paintFocusIndicator(Graphics2D g2) {
        if (!hasFocus()) {
            return;
        }
        Color focus = uiColor("Component.focusColor", new Color(88, 157, 246));
        g2.setColor(focus);
        g2.setStroke(new BasicStroke(1.5f));
        if (chromeSpec.visualStyle() == ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS) {
            int lightDiameter = trafficLightDiameter();
            int diameter = lightDiameter + Math.max(4, lightDiameter / 3);
            g2.drawOval((getWidth() - diameter) / 2, (getHeight() - diameter) / 2, diameter, diameter);
        } else {
            g2.drawRoundRect(3, 3, Math.max(0, getWidth() - 7), Math.max(0, getHeight() - 7), 5, 5);
        }
    }

    private void paintTrafficLight(Graphics2D g2) {
        Color color = windowActive && isEnabled() ? trafficLightColor() : uiColor(
                "MangaReader.macTrafficLightInactive",
                new Color(105, 108, 116)
        );
        if (getModel().isPressed()) {
            color = blend(Color.BLACK, color, 0.20f);
        }

        int diameter = trafficLightDiameter();
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;
        g2.setColor(color);
        g2.fillOval(x, y, diameter, diameter);
        g2.setColor(blend(Color.BLACK, color, 0.16f));
        g2.setStroke(new BasicStroke(Math.max(0.8f, diameter / 15f)));
        g2.drawOval(x, y, diameter, diameter);
    }

    private void paintTrafficLightSymbol(Graphics2D g2) {
        if (!windowActive
                || !isEnabled()
                || (!controlGroupHovered
                    && !getModel().isRollover()
                    && !getModel().isPressed()
                    && !hasFocus())) {
            return;
        }
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int half = Math.max(2, trafficLightDiameter() / 4);
        g2.setColor(uiColor("MangaReader.macTrafficLightGlyph", new Color(52, 42, 37)));
        g2.setStroke(new BasicStroke(
                Math.max(1.15f, trafficLightDiameter() / 10f),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));
        switch (control) {
            case CLOSE -> {
                g2.drawLine(cx - half, cy - half, cx + half, cy + half);
                g2.drawLine(cx + half, cy - half, cx - half, cy + half);
            }
            case MINIMIZE -> g2.drawLine(cx - half, cy, cx + half, cy);
            case MAXIMIZE -> {
                g2.drawLine(cx - half, cy, cx + half, cy);
                g2.drawLine(cx, cy - half, cx, cy + half);
            }
        }
    }

    private Color trafficLightColor() {
        return switch (control) {
            case CLOSE -> uiColor("MangaReader.macTrafficLightClose", new Color(255, 95, 87));
            case MINIMIZE -> uiColor("MangaReader.macTrafficLightMinimize", new Color(254, 188, 46));
            case MAXIMIZE -> uiColor("MangaReader.macTrafficLightZoom", new Color(40, 200, 64));
        };
    }

    private int trafficLightDiameter() {
        int height = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        return Math.max(MINIMUM_TRAFFIC_LIGHT_DIAMETER, Math.round(height * 0.4f));
    }

    private Color standardButtonFeedback(boolean pressed) {
        Color background = getParent() == null
                ? uiColor("Panel.background", new Color(34, 40, 49))
                : getParent().getBackground();
        Color foreground = uiColor("Label.foreground", new Color(233, 233, 233));
        return blend(foreground, background, pressed ? 0.20f : 0.12f);
    }

    private static Color uiColor(String key, Color fallback) {
        Color configured = UIManager.getColor(key);
        return configured == null ? fallback : configured;
    }

    private static Color blend(Color foreground, Color background, float foregroundWeight) {
        float backgroundWeight = 1.0f - foregroundWeight;
        return new Color(
                Math.round(foreground.getRed() * foregroundWeight + background.getRed() * backgroundWeight),
                Math.round(foreground.getGreen() * foregroundWeight + background.getGreen() * backgroundWeight),
                Math.round(foreground.getBlue() * foregroundWeight + background.getBlue() * backgroundWeight),
                foreground.getAlpha()
        );
    }

    private String defaultAccessibleName() {
        return control == Control.MAXIMIZE ? chromeSpec.maximizeAccessibleName() : control.accessibleName();
    }

    private void updateAccessibleText(String text) {
        setToolTipText(text);
        getAccessibleContext().setAccessibleName(text);
        getAccessibleContext().setAccessibleDescription(text);
    }

    public enum Control {
        MINIMIZE("Minimize"),
        MAXIMIZE("Maximize"),
        CLOSE("Close");

        private final String accessibleName;

        Control(String accessibleName) {
            this.accessibleName = accessibleName;
        }

        String accessibleName() {
            return accessibleName;
        }
    }
}
