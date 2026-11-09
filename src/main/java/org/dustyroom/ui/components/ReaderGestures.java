package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Proxy;

/** Precise wheel scrolling, pointer-anchored zoom and grab-to-pan, without image I/O. */
public final class ReaderGestures {
    private ReaderGestures() {}

    public static void install(ImageComponent image, JScrollPane scroll, Runnable changed) {
        scroll.setWheelScrollingEnabled(false);
        scroll.getVerticalScrollBar().setUnitIncrement(48);
        scroll.getHorizontalScrollBar().setUnitIncrement(48);
        Timer save = new Timer(200, event -> changed.run());
        save.setRepeats(false);
        MouseAdapter mouse = new MouseAdapter() {
            private Point pressed, origin;
            private double verticalRemainder, horizontalRemainder;

            @Override public void mouseWheelMoved(MouseWheelEvent event) {
                if (event.isControlDown() || event.isMetaDown()) {
                    Point anchor = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), scroll.getViewport());
                    image.zoomAt(Math.exp(-event.getPreciseWheelRotation() * 0.12), anchor, scroll);
                    save.restart();
                } else {
                    boolean horizontal = event.isShiftDown();
                    JScrollBar bar = horizontal ? scroll.getHorizontalScrollBar() : scroll.getVerticalScrollBar();
                    double delta = event.getPreciseWheelRotation() * (event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                            ? bar.getBlockIncrement() : 48 * Math.max(1, event.getScrollAmount()));
                    double total = delta + (horizontal ? horizontalRemainder : verticalRemainder);
                    int pixels = (int) total;
                    if (horizontal) horizontalRemainder = total - pixels; else verticalRemainder = total - pixels;
                    bar.setValue(bar.getValue() + pixels);
                }
                event.consume();
            }

            @Override public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) && !SwingUtilities.isMiddleMouseButton(event)) return;
                pressed = SwingUtilities.convertPoint(image, event.getPoint(), scroll.getViewport());
                origin = scroll.getViewport().getViewPosition();
                image.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (pressed == null) return;
                Point now = SwingUtilities.convertPoint(image, event.getPoint(), scroll.getViewport());
                JViewport viewport = scroll.getViewport();
                viewport.setViewPosition(new Point(
                        Math.max(0, Math.min(origin.x + pressed.x - now.x, image.getWidth() - viewport.getWidth())),
                        Math.max(0, Math.min(origin.y + pressed.y - now.y, image.getHeight() - viewport.getHeight()))));
            }

            @Override public void mouseReleased(MouseEvent event) {
                pressed = origin = null;
                image.setCursor(Cursor.getDefaultCursor());
            }
        };
        scroll.addMouseWheelListener(mouse);
        image.addMouseListener(mouse);
        image.addMouseMotionListener(mouse);
        installMacMagnification(image, factor -> {
            Point anchor = new Point(scroll.getViewport().getWidth() / 2, scroll.getViewport().getHeight() / 2);
            image.zoomAt(factor, anchor, scroll);
            save.restart();
        });
    }

    private static void installMacMagnification(JComponent component, java.util.function.DoubleConsumer zoom) {
        if (!System.getProperty("os.name", "").startsWith("Mac")) return;
        try {
            Class<?> listener = Class.forName("com.apple.eawt.event.MagnificationListener");
            Class<?> eventClass = Class.forName("com.apple.eawt.event.MagnificationEvent");
            var magnification = eventClass.getMethod("getMagnification");
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, (self, method, args) -> {
                if (method.getName().equals("magnify")) {
                    double factor = Math.max(0.01, 1 + ((Number) magnification.invoke(args[0])).doubleValue());
                    if (SwingUtilities.isEventDispatchThread()) zoom.accept(factor);
                    else SwingUtilities.invokeLater(() -> zoom.accept(factor));
                    return null;
                }
                return switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(self);
                    case "equals" -> self == args[0];
                    case "toString" -> "Yonaka magnification listener";
                    default -> null;
                };
            });
            Class.forName("com.apple.eawt.event.GestureUtilities")
                    .getMethod("addGestureListenerTo", JComponent.class, Class.forName("com.apple.eawt.event.GestureListener"))
                    .invoke(null, component, proxy);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            java.util.logging.Logger.getLogger(ReaderGestures.class.getName())
                    .fine("Native pinch unavailable; Ctrl/Command-wheel zoom remains available: " + exception);
        }
    }
}
