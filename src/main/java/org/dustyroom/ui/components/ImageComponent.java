package org.dustyroom.ui.components;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Setter
@Getter
@NoArgsConstructor
public class ImageComponent extends JComponent {
    private BufferedImage image;
    private double scale = 1.0;
    private FitMode fitMode = FitMode.FIT_HEIGHT;

    public void setImage(BufferedImage image) {
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

        Dimension parentSize = getParent() != null
                ? getParent().getSize()
                : Toolkit.getDefaultToolkit().getScreenSize();

        switch (fitMode) {
            case FIT_WIDTH -> scale = parentSize.getWidth() / image.getWidth();
            case FIT_HEIGHT -> scale = parentSize.getHeight() / image.getHeight();
            case ORIGINAL -> scale = 1.0;
        }
    }

    public void zoomIn() {
        fitMode = FitMode.ORIGINAL;
        scale *= 1.1;
        revalidate();
        repaint();
    }

    public void zoomOut() {
        fitMode = FitMode.ORIGINAL;
        scale /= 1.1;
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
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
        FIT_HEIGHT
    }
}
