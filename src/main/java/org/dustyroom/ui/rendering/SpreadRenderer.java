package org.dustyroom.ui.rendering;

import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SpreadRenderer {

    public String buildTitle(PictureMetadata metadata, Picture secondPage) {
        if (secondPage == null) {
            return String.format("%s - %s", metadata.fileName(), metadata.name());
        }
        return String.format("%s - %s + %s", metadata.fileName(), metadata.name(), secondPage.metadata().name());
    }

    public BufferedImage compose(BufferedImage firstImage, BufferedImage secondImage, boolean rightToLeft) {
        if (secondImage == null) return firstImage;

        BufferedImage leftImage = firstImage;
        BufferedImage rightImage = secondImage;
        if (rightToLeft) {
            leftImage = secondImage;
            rightImage = firstImage;
        }

        int width = leftImage.getWidth() + rightImage.getWidth();
        int height = Math.max(leftImage.getHeight(), rightImage.getHeight());

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);

        int leftY = (height - leftImage.getHeight()) / 2;
        int rightY = (height - rightImage.getHeight()) / 2;
        graphics.drawImage(leftImage, 0, leftY, null);
        graphics.drawImage(rightImage, leftImage.getWidth(), rightY, null);
        graphics.dispose();

        return canvas;
    }
}
