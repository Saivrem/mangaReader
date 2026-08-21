package org.dustyroom.ui.components;

import org.dustyroom.ui.rendering.SpreadRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageComponentTest {

    @Test
    void calculatesFitModesFromExplicitViewport() {
        Dimension viewport = new Dimension(800, 600);
        Dimension portrait = new Dimension(400, 1000);

        assertEquals(2.0, ImageComponent.fitScale(viewport, portrait, ImageComponent.FitMode.FIT_WIDTH));
        assertEquals(0.6, ImageComponent.fitScale(viewport, portrait, ImageComponent.FitMode.FIT_HEIGHT));
        assertEquals(0.6, ImageComponent.fitScale(viewport, portrait, ImageComponent.FitMode.FIT_SCREEN));
        assertEquals(1.0, ImageComponent.fitScale(viewport, portrait, ImageComponent.FitMode.ORIGINAL));
    }

    @Test
    void clampsInvalidAndExtremeFitScales() {
        assertEquals(1.0, ImageComponent.fitScale(
                new Dimension(0, 600),
                new Dimension(400, 1000),
                ImageComponent.FitMode.FIT_SCREEN
        ));
        assertEquals(ImageComponent.MAX_SCALE, ImageComponent.fitScale(
                new Dimension(100_000, 100_000),
                new Dimension(10, 10),
                ImageComponent.FitMode.FIT_SCREEN
        ));
        assertEquals(ImageComponent.MIN_SCALE, ImageComponent.fitScale(
                new Dimension(1, 1),
                new Dimension(100_000, 100_000),
                ImageComponent.FitMode.FIT_SCREEN
        ));
    }

    @Test
    void zoomNeverCreatesUnboundedComponentSizes() {
        ImageComponent component = new ImageComponent();

        for (int index = 0; index < 100; index++) {
            component.zoomIn();
        }
        assertEquals(ImageComponent.MAX_SCALE, component.getScale());

        for (int index = 0; index < 200; index++) {
            component.zoomOut();
        }
        assertEquals(ImageComponent.MIN_SCALE, component.getScale());
    }

    @Test
    @ResourceLock("Swing UIManager defaults")
    void paintsTheThemePageBorderInsideImageBounds() throws Exception {
        Color border = new Color(124, 135, 152);
        AtomicReference<BufferedImage> rendered = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            String key = "MangaReader.imageBorder";
            boolean previouslyPresent = defaults.containsKey(key);
            Object previous = defaults.get(key);
            defaults.put(key, border);
            try {
                BufferedImage page = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
                Graphics2D pageGraphics = page.createGraphics();
                try {
                    pageGraphics.setColor(Color.WHITE);
                    pageGraphics.fillRect(0, 0, page.getWidth(), page.getHeight());
                } finally {
                    pageGraphics.dispose();
                }

                ImageComponent component = new ImageComponent();
                component.setImageAndCenter(page, new JScrollPane(component));
                component.setSize(10, 10);
                BufferedImage target = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
                Graphics2D targetGraphics = target.createGraphics();
                try {
                    component.paint(targetGraphics);
                } finally {
                    targetGraphics.dispose();
                }
                rendered.set(target);
            } finally {
                if (previouslyPresent) {
                    defaults.put(key, previous);
                } else {
                    defaults.remove(key);
                }
            }
        });

        assertEquals(border.getRGB(), rendered.get().getRGB(0, 0));
        assertEquals(Color.WHITE.getRGB(), rendered.get().getRGB(5, 5));
    }

    @Test
    @ResourceLock("Swing UIManager defaults")
    void paintsTransparentSpreadPaddingWithTheOpaqueThemeCanvas() throws Exception {
        Color canvas = new Color(236, 239, 244);
        AtomicReference<BufferedImage> rendered = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BufferedImage tallPage = solidImage(4, 6, Color.RED);
            BufferedImage shortPage = solidImage(4, 2, Color.BLUE);
            BufferedImage spread = new SpreadRenderer().compose(tallPage, shortPage, false);

            ImageComponent component = new ImageComponent();
            component.setOpaque(true);
            component.setBackground(canvas);
            component.setImageAndCenter(spread, new JScrollPane(component));
            component.setSize(spread.getWidth(), spread.getHeight());

            BufferedImage target = new BufferedImage(
                    spread.getWidth(),
                    spread.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = target.createGraphics();
            try {
                component.paint(graphics);
            } finally {
                graphics.dispose();
            }
            rendered.set(target);
        });

        assertEquals(canvas.getRGB(), rendered.get().getRGB(6, 1));
        assertEquals(Color.BLUE.getRGB(), rendered.get().getRGB(6, 2));
        assertEquals(Color.RED.getRGB(), rendered.get().getRGB(2, 2));
    }

    private static BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
