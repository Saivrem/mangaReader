package org.dustyroom.ui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BaseMultiResolutionImage;
import java.io.IOException;
import java.util.List;

/** Decode and resize once on the startup thread, before creating Swing windows. */
public final class BrandAssets {
    private static List<BufferedImage> windowIcons = List.of();
    private static Icon aboutIcon;
    private static Icon welcomeIcon;

    private BrandAssets() {}

    public static void load() {
        try (var stream = BrandAssets.class.getResourceAsStream("/org/dustyroom/ui/branding/yonaka.png")) {
            if (stream == null) throw new IOException("Missing Yonaka icon");
            BufferedImage source = ImageIO.read(stream);
            if (source == null) throw new IOException("Invalid Yonaka icon");
            windowIcons = List.of(resize(source, 32), resize(source, 64), resize(source, 256));
            aboutIcon = multiResolutionIcon(source, 64);
            welcomeIcon = multiResolutionIcon(source, 96);
        } catch (IOException exception) {
            // Branding must never prevent opening a book.
            java.util.logging.Logger.getLogger(BrandAssets.class.getName()).warning(exception.getMessage());
        }
    }

    public static List<BufferedImage> windowIcons() { return windowIcons; }
    public static Icon aboutIcon() { return aboutIcon; }
    public static Icon welcomeIcon() { return welcomeIcon; }

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
            } finally { graphics.dispose(); }
            current = image;
        } while (current.getWidth() > size);
        return current;
    }
}
