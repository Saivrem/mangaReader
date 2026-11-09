package org.dustyroom.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageComponent extends JComponent {
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

    public FitMode getFitMode() {
        return fitMode;
    }

    public void setFitMode(FitMode mode) {
        this.fitMode = mode;
        updateScale();
        revalidate();
        repaint();
    }

    public double getScale() {
        return scale;
    }

    public void restoreView(FitMode mode, double zoom) {
        fitMode = mode;
        scale = zoom;
        updateScale();
        revalidate();
        repaint();
    }

    public void updateScale() {
        if (image == null) return;

        Dimension parentSize = getParent() != null
                ? getParent().getSize()
                : Toolkit.getDefaultToolkit().getScreenSize();

        switch (fitMode) {
            case FIT_WIDTH -> scale = parentSize.getWidth() / image.getWidth();
            case FIT_HEIGHT -> scale = parentSize.getHeight() / image.getHeight();
            case FIT_SCREEN -> scale = Math.min(
                    parentSize.getWidth() / image.getWidth(),
                    parentSize.getHeight() / image.getHeight()
            );
            case ORIGINAL -> { /* Preserve the explicit zoom across page changes. */ }
        }
    }

    public void zoomIn() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.max(0.05, Math.min(20, scale * 1.1));
        revalidate();
        repaint();
    }

    public void zoomOut() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.max(0.05, Math.min(20, scale / 1.1));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // JComponent has no UI delegate to clear an opaque background for us.
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (image == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int imgW = (int) (image.getWidth() * scale);
        int imgH = (int) (image.getHeight() * scale);

        int x = (getWidth() - imgW) / 2;
        int y = (getHeight() - imgH) / 2;

        g2d.drawImage(image, x, y, imgW, imgH, this);
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) return new Dimension(0, 0);
        return new Dimension(
                (int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale)
        );
    }

    public enum FitMode {
        ORIGINAL,
        FIT_WIDTH,
        FIT_HEIGHT,
        FIT_SCREEN
    }
}
