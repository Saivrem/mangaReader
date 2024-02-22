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

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            int imgWidth, imgHeight;
            double panelAspectRatio = (double) panelWidth / panelHeight;
            double imageAspectRatio = (double) image.getWidth() / image.getHeight();

            if (panelAspectRatio > imageAspectRatio) {
                imgWidth = (int) (panelHeight * imageAspectRatio);
                imgHeight = panelHeight;
            } else {
                imgWidth = panelWidth;
                imgHeight = (int) (panelWidth / imageAspectRatio);
            }

            int x = (panelWidth - imgWidth) / 2;
            int y = (panelHeight - imgHeight) / 2;
            g.drawImage(image, x, y, imgWidth, imgHeight, this);
        }
    }
}