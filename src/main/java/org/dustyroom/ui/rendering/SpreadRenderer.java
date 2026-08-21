package org.dustyroom.ui.rendering;

import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.PictureMetadata;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SpreadRenderer {

    public String buildTitle(PictureMetadata metadata, PageRef secondPage) {
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
        try {
            int leftY = (height - leftImage.getHeight()) / 2;
            int rightY = (height - rightImage.getHeight()) / 2;
            graphics.drawImage(leftImage, 0, leftY, null);
            graphics.drawImage(rightImage, leftImage.getWidth(), rightY, null);
        } finally {
            graphics.dispose();
        }

        return canvas;
    }
}
