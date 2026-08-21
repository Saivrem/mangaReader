package org.dustyroom.ui.window;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Owns application-level state and interactions for a permanently undecorated
 * {@link JFrame}. Construct and call this class on the Swing event dispatch
 * thread.
 */
public final class WindowStateController implements AutoCloseable {
    static final String DEFERRED_COMMANDS_PROPERTY = "MangaReader.windowCommands";
    private static final Dimension FALLBACK_MINIMUM_SIZE = new Dimension(360, 240);
    private static final int DRAG_THRESHOLD = 3;

    private final JFrame frame;
    private final WindowChrome chrome;
    private final ScreenBoundsProvider screenBoundsProvider;
    private final WindowStateModel state = new WindowStateModel();
    private final BorderlessFullscreenSurface fullscreenSurface;
    private final DeferredCommands deferredCommands;
    private final MouseAdapter dragHandler = new TitleDragHandler();
    private final MouseAdapter resizeHandler = new BorderResizeHandler();
    private final PropertyChangeListener titleListener = this::titleChanged;
    private final WindowFocusListener focusListener = new WindowFocusListener() {
        @Override
        public void windowGainedFocus(WindowEvent event) {
            chrome.titleBar().setWindowActive(true);
        }

        @Override
        public void windowLostFocus(WindowEvent event) {
            chrome.titleBar().setWindowActive(false);
        }
    };
    private final WindowAdapter lifecycleListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent event) {
            close();
        }
    };

    private boolean closed;

    public WindowStateController(JFrame frame, WindowChrome chrome) {
        this(frame, chrome, ScreenBoundsProvider.system());
    }

    public WindowStateController(
            JFrame frame,
            WindowChrome chrome,
            ScreenBoundsProvider screenBoundsProvider
    ) {
        requireEdt();
        this.frame = Objects.requireNonNull(frame, "frame");
        this.chrome = Objects.requireNonNull(chrome, "chrome");
        this.screenBoundsProvider = Objects.requireNonNull(screenBoundsProvider, "screenBoundsProvider");
        this.fullscreenSurface = new BorderlessFullscreenSurface(new AwtFullscreenHost(frame, chrome));
        if (!frame.isUndecorated()) {
            throw new IllegalArgumentException("WindowStateController requires an undecorated JFrame");
        }
        // FlatLaf recognizes this client property; other look and feels safely
        // ignore it. The application shell is the sole decoration owner.
        frame.getRootPane().putClientProperty("JRootPane.useWindowDecorations", Boolean.FALSE);

        Object commands = chrome.getClientProperty(DEFERRED_COMMANDS_PROPERTY);
        if (!(commands instanceof DeferredCommands deferred)) {
            throw new IllegalArgumentException("WindowChrome does not expose window commands");
        }
        deferredCommands = deferred;
        deferredCommands.bind(this);

        chrome.titleBar().setTitle(frame.getTitle());
        chrome.titleBar().setWindowActive(frame.isFocused());
        chrome.titleBar().dragSurfaces().forEach(component -> {
            component.addMouseListener(dragHandler);
            component.addMouseMotionListener(dragHandler);
        });
        chrome.resizeHandles().forEach(handle -> {
            handle.addMouseListener(resizeHandler);
            handle.addMouseMotionListener(resizeHandler);
        });
        frame.addPropertyChangeListener("title", titleListener);
        frame.addWindowFocusListener(focusListener);
        frame.addWindowListener(lifecycleListener);
    }

    public WindowMode mode() {
        requireEdt();
        return state.mode();
    }

    public boolean isFullscreen() {
        requireEdt();
        return state.mode() == WindowMode.FULLSCREEN;
    }

    public void minimize() {
        requireOpenOnEdt();
        if (isFullscreen()) {
            exitFullscreen();
        }
        frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
    }

    public void toggleMaximized() {
        requireOpenOnEdt();
        if (state.mode() == WindowMode.FULLSCREEN) {
            return;
        }
        if (state.mode() == WindowMode.MAXIMIZED) {
            Rectangle restoreBounds = state.restoreFromMaximized();
            chrome.setMaximized(false);
            frame.setBounds(restoreBounds);
        } else {
            Rectangle currentBounds = frame.getBounds();
            state.maximize(currentBounds);
            ScreenBoundsProvider.ScreenBounds screen = currentScreen(currentBounds);
            frame.setBounds(screen.usableBounds());
            chrome.setMaximized(true);
        }
        frame.revalidate();
        frame.repaint();
    }

    public void requestClose() {
        requireOpenOnEdt();
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    public void toggleFullscreen() {
        requireOpenOnEdt();
        if (isFullscreen()) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    public void enterFullscreen() {
        requireOpenOnEdt();
        if (isFullscreen()) {
            return;
        }

        Rectangle currentBounds = frame.getBounds();
        ScreenBoundsProvider.ScreenBounds screen = currentScreen(currentBounds);
        state.enterFullscreen(currentBounds);
        fullscreenSurface.enter(fullscreenTargetBounds(screen));
    }

    public void exitFullscreen() {
        requireOpenOnEdt();
        if (!isFullscreen()) {
            return;
        }

        WindowStateModel.FullscreenRestore restore = state.exitFullscreen();
        fullscreenSurface.exit(restore.mode(), restore.bounds());
    }

    /** Temporarily exits fullscreen around modal UI and restores it in a finally block. */
    public <T> T withFullscreenSuspended(Supplier<T> operation) {
        requireOpenOnEdt();
        Objects.requireNonNull(operation, "operation");
        boolean resumeFullscreen = isFullscreen();
        if (resumeFullscreen) {
            exitFullscreen();
        }
        try {
            return operation.get();
        } finally {
            if (resumeFullscreen && !closed) {
                enterFullscreen();
            }
        }
    }

    @Override
    public void close() {
        requireEdt();
        if (closed) {
            return;
        }
        if (isFullscreen()) {
            exitFullscreen();
        }
        closed = true;
        deferredCommands.unbind(this);
        chrome.titleBar().dragSurfaces().forEach(component -> {
            component.removeMouseListener(dragHandler);
            component.removeMouseMotionListener(dragHandler);
        });
        chrome.resizeHandles().forEach(handle -> {
            handle.removeMouseListener(resizeHandler);
            handle.removeMouseMotionListener(resizeHandler);
        });
        frame.removePropertyChangeListener("title", titleListener);
        frame.removeWindowFocusListener(focusListener);
        frame.removeWindowListener(lifecycleListener);
    }

    private ScreenBoundsProvider.ScreenBounds currentScreen(Rectangle windowBounds) {
        Point center = WindowGeometry.center(windowBounds);
        return screenBoundsProvider.screenFor(center, windowBounds);
    }

    static Rectangle fullscreenTargetBounds(ScreenBoundsProvider.ScreenBounds screen) {
        return Objects.requireNonNull(screen, "screen").bounds();
    }

    private void titleChanged(PropertyChangeEvent event) {
        chrome.titleBar().setTitle((String) event.getNewValue());
    }

    private Dimension effectiveMinimumSize() {
        Dimension configured = frame.getMinimumSize();
        if (configured == null || configured.width <= 0 || configured.height <= 0) {
            return new Dimension(FALLBACK_MINIMUM_SIZE);
        }
        return new Dimension(configured);
    }

    private void requireOpenOnEdt() {
        requireEdt();
        if (closed) {
            throw new IllegalStateException("WindowStateController is closed");
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("WindowStateController must be used on the EDT");
        }
    }

    /**
     * Applies borderless fullscreen using ordinary window bounds. In
     * particular, this deliberately avoids GraphicsDevice full-screen
     * exclusive mode: switching an already-visible undecorated Swing frame to
     * FSEM can replace or detach its native peer on macOS, leaving the custom
     * Swing content tree present but not composited.
     */
    static final class BorderlessFullscreenSurface {
        private final FullscreenHost host;
        private boolean resizableBeforeFullscreen;
        private boolean active;

        BorderlessFullscreenSurface(FullscreenHost host) {
            this.host = Objects.requireNonNull(host, "host");
        }

        void enter(Rectangle targetBounds) {
            if (active) {
                throw new IllegalStateException("Fullscreen surface is already active");
            }
            Rectangle target = new Rectangle(Objects.requireNonNull(targetBounds, "targetBounds"));
            resizableBeforeFullscreen = host.isResizable();
            host.setResizable(false);
            host.setChromeFullscreen(true);
            host.setBounds(target);
            active = true;
            host.refresh();
            host.requestFocus();
        }

        void exit(WindowMode restoredMode, Rectangle restoredBounds) {
            if (!active) {
                throw new IllegalStateException("Fullscreen surface is not active");
            }
            Rectangle target = new Rectangle(Objects.requireNonNull(restoredBounds, "restoredBounds"));
            host.setChromeFullscreen(false);
            host.setChromeMaximized(restoredMode == WindowMode.MAXIMIZED);
            host.setResizable(resizableBeforeFullscreen);
            host.setBounds(target);
            active = false;
            host.refresh();
            host.requestFocus();
        }
    }

    /** Host seam intentionally exposes neither peer replacement nor visibility mutation. */
    interface FullscreenHost {
        boolean isResizable();

        void setResizable(boolean resizable);

        void setChromeFullscreen(boolean fullscreen);

        void setChromeMaximized(boolean maximized);

        void setBounds(Rectangle bounds);

        void refresh();

        void requestFocus();
    }

    private record AwtFullscreenHost(JFrame frame, WindowChrome chrome) implements FullscreenHost {
        private AwtFullscreenHost {
            Objects.requireNonNull(frame, "frame");
            Objects.requireNonNull(chrome, "chrome");
        }

        @Override
        public boolean isResizable() {
            return frame.isResizable();
        }

        @Override
        public void setResizable(boolean resizable) {
            frame.setResizable(resizable);
        }

        @Override
        public void setChromeFullscreen(boolean fullscreen) {
            chrome.setFullscreen(fullscreen);
        }

        @Override
        public void setChromeMaximized(boolean maximized) {
            chrome.setMaximized(maximized);
        }

        @Override
        public void setBounds(Rectangle bounds) {
            frame.setBounds(bounds);
        }

        @Override
        public void refresh() {
            chrome.revalidate();
            chrome.repaint();
            frame.invalidate();
            frame.validate();
            frame.repaint();
        }

        @Override
        public void requestFocus() {
            frame.requestFocusInWindow();
        }
    }

    private final class TitleDragHandler extends MouseAdapter {
        private Point pressOnScreen;
        private Rectangle initialBounds;
        private boolean pressedWhileMaximized;
        private boolean dragged;

        @Override
        public void mousePressed(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event) || isFullscreen()) {
                return;
            }
            pressOnScreen = event.getLocationOnScreen();
            initialBounds = frame.getBounds();
            pressedWhileMaximized = state.mode() == WindowMode.MAXIMIZED;
            dragged = false;
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (pressOnScreen == null
                    || (event.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0
                    || isFullscreen()) {
                return;
            }
            Point pointer = event.getLocationOnScreen();
            if (!dragged
                    && Math.abs(pointer.x - pressOnScreen.x) < DRAG_THRESHOLD
                    && Math.abs(pointer.y - pressOnScreen.y) < DRAG_THRESHOLD) {
                return;
            }
            dragged = true;

            if (pressedWhileMaximized && state.mode() == WindowMode.MAXIMIZED) {
                Rectangle restoreBounds = state.restoreFromMaximized();
                Rectangle restoredAtPointer = WindowGeometry.restoredBoundsForDrag(
                        restoreBounds,
                        initialBounds,
                        pressOnScreen,
                        pointer
                );
                chrome.setMaximized(false);
                frame.setBounds(restoredAtPointer);
                initialBounds = restoredAtPointer;
                pressOnScreen = pointer;
                pressedWhileMaximized = false;
                return;
            }
            frame.setLocation(WindowGeometry.draggedBounds(initialBounds, pressOnScreen, pointer).getLocation());
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            pressOnScreen = null;
            initialBounds = null;
            pressedWhileMaximized = false;
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event)
                    && event.getClickCount() == 2
                    && !dragged
                    && !isFullscreen()) {
                toggleMaximized();
            }
        }
    }

    private final class BorderResizeHandler extends MouseAdapter {
        private ResizeEdge edge;
        private Point pressOnScreen;
        private Rectangle initialBounds;

        @Override
        public void mousePressed(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)
                    || state.mode() != WindowMode.NORMAL
                    || !frame.isResizable()
                    || !(event.getComponent() instanceof WindowChrome.ResizeHandle handle)) {
                return;
            }
            edge = handle.edge();
            pressOnScreen = event.getLocationOnScreen();
            initialBounds = frame.getBounds();
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (edge == null
                    || pressOnScreen == null
                    || (event.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0
                    || !frame.isResizable()
                    || state.mode() != WindowMode.NORMAL) {
                return;
            }
            frame.setBounds(WindowGeometry.resizedBounds(
                    initialBounds,
                    edge,
                    pressOnScreen,
                    event.getLocationOnScreen(),
                    effectiveMinimumSize()
            ));
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            edge = null;
            pressOnScreen = null;
            initialBounds = null;
        }
    }

    static final class DeferredCommands implements TitleBar.WindowCommands {
        private WindowStateController delegate;

        void bind(WindowStateController controller) {
            if (delegate != null) {
                throw new IllegalStateException("WindowChrome is already bound to a controller");
            }
            delegate = Objects.requireNonNull(controller, "controller");
        }

        void unbind(WindowStateController controller) {
            if (delegate == controller) {
                delegate = null;
            }
        }

        @Override
        public void minimize() {
            requireDelegate().minimize();
        }

        @Override
        public void toggleMaximized() {
            requireDelegate().toggleMaximized();
        }

        @Override
        public void requestClose() {
            requireDelegate().requestClose();
        }

        private WindowStateController requireDelegate() {
            if (delegate == null) {
                throw new IllegalStateException("WindowChrome is not bound to a controller");
            }
            return delegate;
        }
    }
}
