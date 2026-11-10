package org.dustyroom.ui.components;

import org.dustyroom.be.animation.AnimationDecoder;
import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.animation.AnimationClip;
import org.dustyroom.be.models.animation.AnimationFrame;
import org.dustyroom.be.models.state.FitMode;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AnimationRenderingTest {
    @Test
    void unchangedFramesDoNotFloodTheEventThreadAndPauseClosesTheSource() throws Exception {
        AtomicInteger paints = new AtomicInteger();
        CountDownLatch closed = new CountDownLatch(1);
        ImageComponent component = new ImageComponent() {
            @Override public boolean isShowing() { return true; }
            @Override public void repaint() { paints.incrementAndGet(); }
        };
        BufferedImage source = solid(Color.RED);
        BufferedImage animated = solid(Color.BLUE);
        AnimationClip clip = new AnimationClip(() -> new AnimationDecoder() {
            public AnimationFrame next() { return new AnimationFrame(animated, 5000); }
            public int loops() { return 0; }
            public void close() { closed.countDown(); }
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                component.setBackground(Color.BLACK);
                JScrollPane scroll = new JScrollPane(component);
                scroll.setSize(40, 40);
                scroll.doLayout();
                component.setImageAndCenter(source, scroll);
                component.restoreView(FitMode.ORIGINAL, 1);
                component.setSize(20, 20);
                component.setAnimations(List.of(new AnimatedPage(clip, 0, 0, 20, 20, false)));
            });
            long timeout = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            AtomicInteger pixel = new AtomicInteger();
            do {
                SwingUtilities.invokeAndWait(() -> pixel.set(render(component).getRGB(10, 10)));
                if (pixel.get() == Color.BLUE.getRGB()) break;
                Thread.sleep(5);
            } while (System.nanoTime() < timeout);
            assertEquals(Color.BLUE.getRGB(), pixel.get());
            SwingUtilities.invokeAndWait(() -> paints.set(0));
            Thread.sleep(150);
            SwingUtilities.invokeAndWait(() -> assertEquals(0, paints.get(), "Unchanged GIF must not request repaints"));
            SwingUtilities.invokeAndWait(() -> component.setAnimationPaused(true));
            assertTrue(closed.await(2, TimeUnit.SECONDS), "Loading another file must release the old decoder");
            SwingUtilities.invokeAndWait(() -> component.setImageAndCenter(solid(Color.GREEN), new JScrollPane(component)));
        } finally {
            SwingUtilities.invokeAndWait(() -> component.setAnimations(List.of()));
        }
    }

    @Test
    void pausedComponentDoesNotStartDecodingWhileAnotherFileIsLoading() throws Exception {
        AtomicInteger opens = new AtomicInteger();
        ImageComponent component = new ImageComponent() {
            @Override public boolean isShowing() { return true; }
        };
        AnimationClip clip = new AnimationClip(() -> {
            opens.incrementAndGet();
            throw new AssertionError("Decoder should remain paused");
        });
        SwingUtilities.invokeAndWait(() -> {
            component.setAnimationPaused(true);
            component.setAnimations(List.of(new AnimatedPage(clip, 0, 0, 20, 20, false)));
        });
        Thread.sleep(100);
        assertEquals(0, opens.get());
        SwingUtilities.invokeAndWait(() -> component.setAnimations(List.of()));
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 20, 20);
        graphics.dispose();
        return image;
    }

    private static BufferedImage render(ImageComponent component) {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
}
