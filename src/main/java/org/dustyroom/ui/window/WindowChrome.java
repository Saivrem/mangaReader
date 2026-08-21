package org.dustyroom.ui.window;

import org.dustyroom.ui.window.platform.ChromeSpec;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Content shell for a permanently undecorated frame. The menu bar is part of
 * the shell rather than installed through JFrame#setJMenuBar, keeping the
 * application title bar above it on every platform.
 */
public final class WindowChrome extends JPanel {
    private static final int FALLBACK_RESIZE_HANDLE_SIZE = 5;

    private final TitleBar titleBar;
    private final JMenuBar menuBar;
    private final Component footer;
    private final JPanel body;
    private final Map<ResizeEdge, ResizeHandle> resizeHandles = new EnumMap<>(ResizeEdge.class);
    private boolean fullscreen;
    private boolean maximized;
    private boolean titleWasVisible;
    private boolean menuWasVisible;
    private boolean footerWasVisible;

    public WindowChrome(Component content, JMenuBar menuBar, Component footer) {
        this(content, menuBar, footer, ChromeSpec.current());
    }

    /**
     * Creates chrome with an explicit platform policy. This overload is useful
     * for deterministic integration tests and platform previews.
     */
    public WindowChrome(
            Component content,
            JMenuBar menuBar,
            Component footer,
            ChromeSpec chromeSpec
    ) {
        super(null);
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(chromeSpec, "chromeSpec");
        this.menuBar = menuBar;
        this.footer = footer;

        WindowStateController.DeferredCommands commands = new WindowStateController.DeferredCommands();
        titleBar = new TitleBar(commands, chromeSpec);
        putClientProperty(WindowStateController.DEFERRED_COMMANDS_PROPERTY, commands);

        body = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(titleBar, BorderLayout.NORTH);
        if (menuBar != null) {
            top.add(menuBar, BorderLayout.SOUTH);
        }
        body.add(top, BorderLayout.NORTH);
        body.add(content, BorderLayout.CENTER);
        if (footer != null) {
            body.add(footer, BorderLayout.SOUTH);
        }

        add(body);
        for (ResizeEdge edge : ResizeEdge.values()) {
            ResizeHandle handle = new ResizeHandle(edge);
            resizeHandles.put(edge, handle);
            add(handle, 0);
        }
        applyTheme();
    }

    public TitleBar titleBar() {
        return titleBar;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isMaximized() {
        return maximized;
    }

    void setFullscreen(boolean fullscreen) {
        if (this.fullscreen == fullscreen) {
            return;
        }
        this.fullscreen = fullscreen;
        if (fullscreen) {
            titleWasVisible = titleBar.isVisible();
            menuWasVisible = menuBar != null && menuBar.isVisible();
            footerWasVisible = footer != null && footer.isVisible();
            titleBar.setVisible(false);
            if (menuBar != null) {
                menuBar.setVisible(false);
            }
            if (footer != null) {
                footer.setVisible(false);
            }
        } else {
            titleBar.setVisible(titleWasVisible);
            if (menuBar != null) {
                menuBar.setVisible(menuWasVisible);
            }
            if (footer != null) {
                footer.setVisible(footerWasVisible);
            }
        }
        updateResizeHandles();
        revalidate();
        repaint();
    }

    void setMaximized(boolean maximized) {
        this.maximized = maximized;
        titleBar.setMaximized(maximized);
        updateResizeHandles();
        revalidate();
        repaint();
    }

    List<ResizeHandle> resizeHandles() {
        return List.copyOf(resizeHandles.values());
    }

    @Override
    public void doLayout() {
        int width = getWidth();
        int height = getHeight();
        body.setBounds(0, 0, width, height);

        int thickness = resizeHandleThickness();
        int horizontalLength = Math.max(0, width - thickness * 2);
        int verticalLength = Math.max(0, height - thickness * 2);
        setHandleBounds(ResizeEdge.NORTH_WEST, 0, 0, thickness, thickness);
        setHandleBounds(ResizeEdge.NORTH, thickness, 0, horizontalLength, thickness);
        setHandleBounds(ResizeEdge.NORTH_EAST, Math.max(0, width - thickness), 0, thickness, thickness);
        setHandleBounds(ResizeEdge.EAST, Math.max(0, width - thickness), thickness, thickness, verticalLength);
        setHandleBounds(
                ResizeEdge.SOUTH_EAST,
                Math.max(0, width - thickness),
                Math.max(0, height - thickness),
                thickness,
                thickness
        );
        setHandleBounds(ResizeEdge.SOUTH, thickness, Math.max(0, height - thickness), horizontalLength, thickness);
        setHandleBounds(
                ResizeEdge.SOUTH_WEST,
                0,
                Math.max(0, height - thickness),
                thickness,
                thickness
        );
        setHandleBounds(ResizeEdge.WEST, 0, thickness, thickness, verticalLength);
    }

    @Override
    public Dimension getPreferredSize() {
        return body == null ? super.getPreferredSize() : new Dimension(body.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return body == null ? super.getMinimumSize() : new Dimension(body.getMinimumSize());
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (resizeHandles != null) {
            applyTheme();
        }
    }

    private void setHandleBounds(ResizeEdge edge, int x, int y, int width, int height) {
        resizeHandles.get(edge).setBounds(new Rectangle(x, y, width, height));
    }

    private void updateResizeHandles() {
        boolean visible = !fullscreen && !maximized;
        resizeHandles.values().forEach(handle -> handle.setVisible(visible));
    }

    private void applyTheme() {
        Color background = UIManager.getColor("MangaReader.titleBarBackground");
        if (background == null) {
            background = UIManager.getColor("Panel.background");
        }
        setBackground(background == null ? new Color(34, 40, 49) : background);
        revalidate();
        repaint();
    }

    private static int resizeHandleThickness() {
        int configured = UIManager.getInt("MangaReader.windowResizeHandleThickness");
        return configured > 0 ? configured : FALLBACK_RESIZE_HANDLE_SIZE;
    }

    static final class ResizeHandle extends JComponent {
        private final ResizeEdge edge;

        ResizeHandle(ResizeEdge edge) {
            this.edge = edge;
            setCursor(Cursor.getPredefinedCursor(cursorFor(edge)));
            setOpaque(false);
        }

        ResizeEdge edge() {
            return edge;
        }

        @Override
        public Dimension getPreferredSize() {
            int size = resizeHandleThickness();
            return switch (edge) {
                case NORTH, SOUTH -> new Dimension(0, size);
                case EAST, WEST -> new Dimension(size, 0);
                default -> new Dimension(size, size);
            };
        }

        private static int cursorFor(ResizeEdge edge) {
            return switch (edge) {
                case NORTH -> Cursor.N_RESIZE_CURSOR;
                case NORTH_EAST -> Cursor.NE_RESIZE_CURSOR;
                case EAST -> Cursor.E_RESIZE_CURSOR;
                case SOUTH_EAST -> Cursor.SE_RESIZE_CURSOR;
                case SOUTH -> Cursor.S_RESIZE_CURSOR;
                case SOUTH_WEST -> Cursor.SW_RESIZE_CURSOR;
                case WEST -> Cursor.W_RESIZE_CURSOR;
                case NORTH_WEST -> Cursor.NW_RESIZE_CURSOR;
            };
        }
    }
}
