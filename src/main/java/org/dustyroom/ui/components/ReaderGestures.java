package org.dustyroom.ui.components;

import org.dustyroom.be.utils.OSUtils;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.DoubleConsumer;
import java.util.logging.Logger;

/**
 * Precise wheel scrolling, pointer-anchored zoom and grab-to-pan, without image I/O.
 */
@UtilityClass
public class ReaderGestures {

    public static void install(ImageComponent image, JScrollPane scroll, Runnable changed) {
        scroll.setWheelScrollingEnabled(false);
        scroll.getVerticalScrollBar().setUnitIncrement(48);
        scroll.getHorizontalScrollBar().setUnitIncrement(48);
        Timer save = new Timer(200, _ -> changed.run());
        save.setRepeats(false);
        MouseAdapter mouse = new ReaderMouseGestures(image, scroll, save);
        scroll.addMouseWheelListener(mouse);
        image.addMouseListener(mouse);
        image.addMouseMotionListener(mouse);
        installMacMagnification(image, factor -> {
            Point anchor = new Point(scroll.getViewport().getWidth() / 2, scroll.getViewport().getHeight() / 2);
            image.zoomAt(factor, anchor, scroll);
            save.restart();
        });
    }

    private static void installMacMagnification(JComponent component, DoubleConsumer zoom) {
        if (!OSUtils.isMac(OSUtils.currentName())) {
            return;
        }
        try {
            Class<?> listener = Class.forName("com.apple.eawt.event.MagnificationListener");
            Class<?> eventClass = Class.forName("com.apple.eawt.event.MagnificationEvent");
            Method magnification = eventClass.getMethod("getMagnification");
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, (self, method, args) -> {
                if (method.getName().equals("magnify")) {
                    double factor = Math.max(0.01, 1 + ((Number) magnification.invoke(args[0])).doubleValue());
                    if (SwingUtilities.isEventDispatchThread()) {
                        zoom.accept(factor);
                    } else {
                        SwingUtilities.invokeLater(() -> zoom.accept(factor));
                    }
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
            Logger.getLogger(ReaderGestures.class.getName())
                    .fine("Native pinch unavailable; Ctrl/Command-wheel zoom remains available: " + exception);
        }
    }
}
