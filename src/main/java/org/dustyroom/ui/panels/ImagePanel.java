package org.dustyroom.ui.panels;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class ImagePanel extends JPanel {

    private BufferedImage image;
    private boolean fitMode = true;
    private double scale = 1.0;

    public void toggleZoomMode() {
        fitMode = !fitMode;
        if (fitMode) {
            scale = fitScale();
        } else {
            // Set an initial zoom level, or keep the current scale if already in zoom mode
            scale = (scale == 1.0) ? 1.2 : scale;
        }
    }

    public double fitScale() {
        double panelAspectRatio = (double) getWidth() / getHeight();
        double imageAspectRatio = (double) image.getWidth(this) / image.getHeight(this);

        return (panelAspectRatio > imageAspectRatio)
                ? (double) getHeight() / image.getHeight(this)
                : (double) getWidth() / image.getWidth(this);
    }

    public void zoomIn() {
        scale *= 1.1;
    }

    public void zoomOut() {
        scale /= 1.1;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            /*if (fitMode) {
                fitImage(g);
            } else {
                zoomImage(g);
            }*/
            drawImage(g, fitMode);
        }
    }

    private void drawImage(Graphics g, boolean fitMode) {
        Graphics2D g2d = (Graphics2D) g.create();

        int imageWidth, imageHeight;
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (fitMode) {
            double panelAspectRatio = (double) panelWidth / panelHeight;
            double imageAspectRatio = (double) image.getWidth(this) / image.getHeight(this);

            if (panelAspectRatio > imageAspectRatio) {
                imageWidth = (int) (panelHeight * imageAspectRatio);
                imageHeight = panelHeight;
            } else {
                imageWidth = panelWidth;
                imageHeight = (int) (panelWidth / imageAspectRatio);
            }
        } else {
            imageWidth = (int) (image.getWidth(this) * scale);
            imageHeight = (int) (image.getHeight(this) * scale);
        }

        int x = (panelWidth - imageWidth) / 2;
        int y = (panelHeight - imageHeight) / 2;

        g2d.drawImage(image, x, y, imageWidth, imageHeight, this);
        g2d.dispose();
    }
}