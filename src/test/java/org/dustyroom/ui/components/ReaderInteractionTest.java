package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReaderInteractionTest {
    @Test
    void pageIsCenteredSynchronouslyAndDoesNotJumpOnTheNextEdtTurn() throws Exception {
        Point[] expected = new Point[1];
        JScrollPane[] scroller = new JScrollPane[1];
        SwingUtilities.invokeAndWait(() -> {
            ImageComponent image = new ImageComponent();
            JScrollPane scroll = new JScrollPane(image);
            scroller[0] = scroll;
            scroll.setSize(600, 400);
            image.restoreView(ImageComponent.FitMode.ORIGINAL, 1);
            image.setImageAndCenter(new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB), scroll);
            scroll.getViewport().setViewPosition(new Point(11, 90));
            image.setImageAndCenter(new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB), scroll);
            expected[0] = new Point((1000 - scroll.getViewport().getWidth()) / 2, 0);
            assertEquals(expected[0], scroll.getViewport().getViewPosition());
        });
        SwingUtilities.invokeAndWait(() -> assertEquals(expected[0], scroller[0].getViewport().getViewPosition()));
    }

    @Test
    void zoomKeepsThePointUnderThePointerAndClampsScale() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImageComponent image = new ImageComponent();
            JScrollPane scroll = new JScrollPane(image);
            scroll.setSize(600, 400);
            image.restoreView(ImageComponent.FitMode.ORIGINAL, 1);
            image.setImageAndCenter(new BufferedImage(1200, 1000, BufferedImage.TYPE_INT_RGB), scroll);
            scroll.getViewport().setViewPosition(new Point(200, 150));
            Point anchor = new Point(100, 100);
            image.zoomAt(2, anchor, scroll);
            assertEquals(2, image.getScale());
            assertEquals(new Point(500, 400), scroll.getViewport().getViewPosition());
            image.zoomAt(100, anchor, scroll);
            assertEquals(20, image.getScale());
            image.zoomAt(0.00001, anchor, scroll);
            assertEquals(0.05, image.getScale());
        });
    }

    @Test
    void preciseWheelScrollAndDraggingMoveTheViewport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImageComponent image = new ImageComponent();
            JScrollPane scroll = new JScrollPane(image);
            scroll.setSize(600, 400);
            image.restoreView(ImageComponent.FitMode.ORIGINAL, 1);
            image.setImageAndCenter(new BufferedImage(1200, 1000, BufferedImage.TYPE_INT_RGB), scroll);
            ReaderGestures.install(image, scroll, () -> {
            });
            var wheel = new MouseWheelEvent(scroll, MouseEvent.MOUSE_WHEEL, 0, 0, 100, 100,
                    100, 100, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1, 0.25);
            scroll.dispatchEvent(wheel);
            assertEquals(36, scroll.getViewport().getViewPosition().y);
            Point before = scroll.getViewport().getViewPosition();
            image.dispatchEvent(new MouseEvent(image, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK,
                    100, 100, 1, false, MouseEvent.BUTTON1));
            image.dispatchEvent(new MouseEvent(image, MouseEvent.MOUSE_DRAGGED, 1, InputEvent.BUTTON1_DOWN_MASK,
                    70, 70, 0, false, MouseEvent.NOBUTTON));
            assertEquals(new Point(before.x + 30, before.y + 30), scroll.getViewport().getViewPosition());
            image.dispatchEvent(new MouseEvent(image, MouseEvent.MOUSE_RELEASED, 2, 0, 70, 70, 1, false, MouseEvent.BUTTON1));
        });
    }
}
