package org.dustyroom.ui.components;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
public class ImageComponent extends JComponent {
    static final double MIN_SCALE = 0.05;
    static final double MAX_SCALE = 8.0;

    private BufferedImage image;
    private double scale = 1.0;
    private FitMode fitMode = FitMode.FIT_SCREEN;

    public void setImageAndCenter(BufferedImage img, JScrollPane scrollPane) {
        setImage(img);

        SwingUtilities.invokeLater(() -> {
            Dimension viewSize = scrollPane.getViewport().getExtentSize();
            Dimension imgSize = getPreferredSize();

            int x = Math.max(0, (imgSize.width - viewSize.width) / 2);
            int y = 0;

            scrollPane.getViewport().setViewPosition(new Point(x, y));
        });
    }

    private void setImage(BufferedImage image) {
        this.image = image;
        updateScale();
        revalidate();
        repaint();
    }

    public void setFitMode(FitMode mode) {
        this.fitMode = mode;
        updateScale();
        revalidate();
        repaint();
    }

    public void updateScale() {
        if (image == null) return;

        Container parent = getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) {
            return;
        }
        Dimension parentSize = parent.getSize();

        scale = fitScale(parentSize, new Dimension(image.getWidth(), image.getHeight()), fitMode);
    }

    public void zoomIn() {
        fitMode = FitMode.ORIGINAL;
        scale = clampScale(scale * 1.1);
        revalidate();
        repaint();
    }

    public void zoomOut() {
        fitMode = FitMode.ORIGINAL;
        scale = clampScale(scale / 1.1);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            if (isOpaque() && getBackground() != null) {
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
            if (image == null) return;

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int imgW = scaledDimension(image.getWidth());
            int imgH = scaledDimension(image.getHeight());

            int x = (getWidth() - imgW) / 2;
            int y = (getHeight() - imgH) / 2;

            g2d.drawImage(image, x, y, imgW, imgH, this);
            Color border = UIManager.getColor("MangaReader.imageBorder");
            if (border != null) {
                g2d.setColor(border);
                g2d.drawRect(x, y, Math.max(0, imgW - 1), Math.max(0, imgH - 1));
            }
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) return new Dimension(0, 0);
        return new Dimension(
                scaledDimension(image.getWidth()),
                scaledDimension(image.getHeight())
        );
    }

    static double fitScale(Dimension viewport, Dimension image, FitMode mode) {
        if (viewport.width <= 0 || viewport.height <= 0 || image.width <= 0 || image.height <= 0) {
            return 1.0;
        }
        double calculated = switch (mode) {
            case FIT_WIDTH -> viewport.getWidth() / image.getWidth();
            case FIT_HEIGHT -> viewport.getHeight() / image.getHeight();
            case FIT_SCREEN -> Math.min(
                    viewport.getWidth() / image.getWidth(),
                    viewport.getHeight() / image.getHeight()
            );
            case ORIGINAL -> 1.0;
        };
        return clampScale(calculated);
    }

    private static double clampScale(double candidate) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, candidate));
    }

    private int scaledDimension(int sourceDimension) {
        long rounded = Math.round(sourceDimension * scale);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, rounded));
    }

    public enum FitMode {
        ORIGINAL,
        FIT_WIDTH,
        FIT_HEIGHT,
        FIT_SCREEN
    }
}
