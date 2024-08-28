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
public class ImagePanel extends JPanel {
    boolean fitToHeightEnabled = true;
    boolean fitToWidthEnabled = false;
    private BufferedImage image;
    private double scale = 1.0;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.scale(scale, scale);
        g2d.drawImage(image, 0, 0, this);
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            return new Dimension(newWidth, newHeight);
        }
        return new Dimension(800, 600);
    }

    public void fitImageToHeight() {
        double panelHeight = this.getParent().getHeight();
        double imageHeight = image.getHeight();
        scale = panelHeight / imageHeight;
        fitToHeightEnabled = true;
        fitToWidthEnabled = false;
        this.revalidate();
        this.repaint();
    }

    public void fitImageToWidth() {
        double panelWidth = this.getParent().getWidth();
        double imageWidth = image.getWidth();
        scale = panelWidth / imageWidth;
        fitToHeightEnabled = false;
        fitToWidthEnabled = true;
        this.revalidate();
        this.repaint();
    }

    public void drawImage(BufferedImage image) {
        if (image == null) return;
        this.image = image;
        if (fitToHeightEnabled) {
            fitImageToHeight();
        } else if (fitToWidthEnabled) {
            fitImageToWidth();
        }
        revalidate();
        repaint();
    }

    public void zoomIn() {
        scale *= 1.1;
        fitToHeightEnabled = false;
        fitToWidthEnabled = false;
        revalidate();
        repaint();
    }

    public void zoomOut() {
        scale /= 1.1;
        fitToHeightEnabled = false;
        fitToWidthEnabled = false;
        revalidate();
        repaint();
    }
}
