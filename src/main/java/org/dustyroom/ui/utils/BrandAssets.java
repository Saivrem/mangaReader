package org.dustyroom.ui.utils;

import lombok.Getter;
import lombok.experimental.Accessors;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * Decode and resize once on the startup thread, before creating Swing windows.
 */
@UtilityClass
public class BrandAssets {
    @Getter
    @Accessors(fluent = true)
    private static List<BufferedImage> windowIcons = List.of();
    @Getter
    @Accessors(fluent = true)
    private static Icon aboutIcon;
    @Getter
    @Accessors(fluent = true)
    private static Icon welcomeIcon;

    public static void load() {
        try (InputStream stream = BrandAssets.class.getResourceAsStream("/org/dustyroom/ui/branding/yonaka-original.png")) {
            if (stream == null) {
                throw new IOException("Missing Yonaka icon");
            }
            BufferedImage source = ImageIO.read(stream);
            if (source == null) {
                throw new IOException("Invalid Yonaka icon");
            }
            windowIcons = List.of(resize(source, 32), resize(source, 64), resize(source, 256));
            aboutIcon = multiResolutionIcon(source, 64);
            welcomeIcon = multiResolutionIcon(source, 96);
        } catch (IOException exception) {
            // Branding must never prevent opening a book.
            Logger.getLogger(BrandAssets.class.getName()).warning(exception.getMessage());
        }
    }

    private static Icon multiResolutionIcon(BufferedImage source, int logicalSize) {
        return new ImageIcon(new BaseMultiResolutionImage(resize(source, logicalSize),
                resize(source, logicalSize * 2), resize(source, logicalSize * 3)));
    }

    private static BufferedImage resize(BufferedImage source, int size) {
        BufferedImage current = source;
        do {
            int nextSize = Math.max(size, current.getWidth() / 2);
            BufferedImage image = new BufferedImage(nextSize, nextSize, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(current, 0, 0, nextSize, nextSize, null);
            } finally {
                graphics.dispose();
            }
            current = image;
        } while (current.getWidth() > size);
        return current;
    }
}
