package org.dustyroom.ui.window;

import org.dustyroom.ui.window.platform.ChromeSpec;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Application-painted title bar with platform-adaptive window controls. */
public final class TitleBar extends JPanel {
    private static final int FALLBACK_HEIGHT = 30;

    private final ChromeSpec chromeSpec;
    private final JLabel titleLabel = new JLabel();
    private final WindowControlButton minimizeButton;
    private final WindowControlButton maximizeButton;
    private final WindowControlButton closeButton;
    private boolean windowActive = true;
    private Color borderColor;
    private List<Component> dragSurfaces;

    TitleBar(WindowCommands commands) {
        this(commands, ChromeSpec.current());
    }

    TitleBar(WindowCommands commands, ChromeSpec chromeSpec) {
        super(new BorderLayout());
        this.chromeSpec = Objects.requireNonNull(chromeSpec, "chromeSpec");
        setOpaque(true);

        boolean centeredTitle = chromeSpec.titleAlignment() == ChromeSpec.TitleAlignment.CENTER;
        titleLabel.setHorizontalAlignment(centeredTitle ? JLabel.CENTER : JLabel.LEADING);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, centeredTitle ? 8 : 10, 0, 8));
        add(titleLabel, BorderLayout.CENTER);

        minimizeButton = new WindowControlButton(
                WindowControlButton.Control.MINIMIZE,
                commands::minimize,
                chromeSpec
        );
        maximizeButton = new WindowControlButton(
                WindowControlButton.Control.MAXIMIZE,
                commands::toggleMaximized,
                chromeSpec
        );
        closeButton = new WindowControlButton(
                WindowControlButton.Control.CLOSE,
                commands::requestClose,
                chromeSpec
        );

        JPanel controls = new JPanel(new GridLayout(1, 3, 0, 0));
        controls.setOpaque(false);
        for (ChromeSpec.ControlRole role : chromeSpec.controlOrder()) {
            controls.add(buttonFor(role));
        }
        if (chromeSpec.visualStyle() == ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS) {
            controlButtons().forEach(button -> button.getModel().addChangeListener(
                    event -> synchronizeTrafficLightHover()
            ));
        }
        int outerMargin = controlsOuterMargin();
        if (chromeSpec.controlPlacement() == ChromeSpec.ControlPlacement.LEFT) {
            controls.setBorder(BorderFactory.createEmptyBorder(0, outerMargin, 0, 0));
            add(controls, BorderLayout.WEST);
        } else {
            controls.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, outerMargin));
            add(controls, BorderLayout.EAST);
        }

        List<Component> draggable = new ArrayList<>();
        draggable.add(this);
        draggable.add(titleLabel);
        // Mouse events from buttons do not bubble to their parent. Registering
        // the cluster therefore makes only its empty insets draggable while
        // preserving each button as an interaction exclusion zone.
        draggable.add(controls);
        if (centeredTitle) {
            JComponent balancingSpacer = mirrorOf(controls);
            String oppositeSide = chromeSpec.controlPlacement() == ChromeSpec.ControlPlacement.LEFT
                    ? BorderLayout.EAST
                    : BorderLayout.WEST;
            add(balancingSpacer, oppositeSide);
            draggable.add(balancingSpacer);
        }
        dragSurfaces = List.copyOf(draggable);

        applyTheme();
    }

    ChromeSpec chromeSpec() {
        return chromeSpec;
    }

    public String title() {
        return titleLabel.getText();
    }

    public void setTitle(String title) {
        titleLabel.setText(title == null ? "" : title);
        titleLabel.setToolTipText(title == null || title.isBlank() ? null : title);
    }

    public boolean isWindowActive() {
        return windowActive;
    }

    public void setWindowActive(boolean active) {
        windowActive = active;
        minimizeButton.setWindowActive(active);
        maximizeButton.setWindowActive(active);
        closeButton.setWindowActive(active);
        applyTheme();
    }

    void setMaximized(boolean maximized) {
        maximizeButton.setMaximized(maximized);
    }

    List<Component> dragSurfaces() {
        return dragSurfaces;
    }

    List<WindowControlButton> controlButtons() {
        return List.of(minimizeButton, maximizeButton, closeButton);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (titleLabel != null) {
            applyTheme();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int configuredHeight = UIManager.getInt("MangaReader.titleBarHeight");
        int height = configuredHeight > 0 ? configuredHeight : FALLBACK_HEIGHT;
        return new Dimension(preferred.width, Math.max(preferred.height, height));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (borderColor != null && getHeight() > 0) {
            graphics.setColor(borderColor);
            graphics.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    private void applyTheme() {
        Color background = uiColor(
                "MangaReader.titleBarBackground",
                uiColor("MenuBar.background", uiColor("Panel.background", new Color(34, 40, 49)))
        );
        Color foreground = uiColor(
                "MangaReader.titleBarForeground",
                uiColor("Label.foreground", new Color(233, 233, 233))
        );
        Color border = uiColor(
                "MangaReader.titleBarBorder",
                uiColor("Separator.foreground", background.darker())
        );
        if (!windowActive) {
            background = uiColor("MangaReader.titleBarInactiveBackground", background.darker());
            foreground = uiColor(
                    "MangaReader.titleBarInactiveForeground",
                    new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 150)
            );
        }
        setBackground(background);
        borderColor = border;
        titleLabel.setForeground(foreground);
        repaint();
    }

    private static Color uiColor(String key, Color fallback) {
        Color configured = UIManager.getColor(key);
        return configured == null ? fallback : configured;
    }

    private WindowControlButton buttonFor(ChromeSpec.ControlRole role) {
        return switch (role) {
            case CLOSE -> closeButton;
            case MINIMIZE -> minimizeButton;
            case MAXIMIZE -> maximizeButton;
        };
    }

    private void synchronizeTrafficLightHover() {
        boolean hovered = controlButtons().stream().anyMatch(button -> button.getModel().isRollover());
        controlButtons().forEach(button -> button.setControlGroupHovered(hovered));
    }

    private int controlsOuterMargin() {
        if (chromeSpec.visualStyle() != ChromeSpec.VisualStyle.MACOS_TRAFFIC_LIGHTS) {
            return 0;
        }
        Object configured = UIManager.get("MangaReader.macTitleBarControlsInset");
        return configured instanceof Number number ? Math.max(0, number.intValue()) : 4;
    }

    private static JComponent mirrorOf(JComponent component) {
        JComponent spacer = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(component.getPreferredSize());
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(component.getMinimumSize());
            }
        };
        spacer.setOpaque(false);
        return spacer;
    }

    interface WindowCommands {
        void minimize();

        void toggleMaximized();

        void requestClose();
    }
}
