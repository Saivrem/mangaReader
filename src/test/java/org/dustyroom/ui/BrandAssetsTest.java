package org.dustyroom.ui;

import org.junit.jupiter.api.Test;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import static org.junit.jupiter.api.Assertions.*;

class BrandAssetsTest {
    @Test void iconsKeepLogicalDimensionsAndProvideRetinaPixels() {
        BrandAssets.load();
        for (var icon : new javax.swing.Icon[]{BrandAssets.aboutIcon(), BrandAssets.welcomeIcon()}) {
            assertNotNull(icon);
            var image = assertInstanceOf(MultiResolutionImage.class, ((ImageIcon) icon).getImage());
            int size = icon.getIconWidth();
            assertEquals(size, icon.getIconHeight());
            assertEquals(size * 2, image.getResolutionVariant(size * 2, size * 2).getWidth(null));
            assertEquals(size * 3, image.getResolutionVariant(size * 3, size * 3).getWidth(null));
            BufferedImage screen = new BufferedImage(size * 2, size * 2, BufferedImage.TYPE_INT_ARGB);
            var graphics = screen.createGraphics();
            try { graphics.scale(2, 2); icon.paintIcon(null, graphics, 0, 0); }
            finally { graphics.dispose(); }
            BufferedImage retina = (BufferedImage) image.getResolutionVariant(size * 2, size * 2);
            // Compare the actual painted pixels, not just metadata: Swing must choose the 2x image.
            for (int y = size / 2; y < size * 3 / 2; y++) {
                for (int x = size / 2; x < size * 3 / 2; x++) {
                    assertEquals(retina.getRGB(x, y), screen.getRGB(x, y));
                }
            }
        }
    }
}
