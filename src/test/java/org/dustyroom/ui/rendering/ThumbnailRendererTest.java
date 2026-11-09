package org.dustyroom.ui.rendering;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailRendererTest {
    @Test
    void averagesFineScreentonesInsteadOfDroppingThem() {
        BufferedImage page = new BufferedImage(3456, 4608, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) page.setRGB(x, y, ((x + y) % 2 == 0) ? 0 : 0xffffff);
        }
        BufferedImage preview = ThumbnailRenderer.render(page);
        int gray = preview.getRGB(preview.getWidth() / 2, preview.getHeight() / 2) & 255;
        assertTrue(gray >= 120 && gray <= 136, "Fine screentone should average to gray: " + gray);
    }

    @Test
    void boundsPortraitLandscapeAndVeryThinPagesWithoutUpscaling() {
        for (int[] size : new int[][]{{1000, 2000}, {2000, 1000}, {1, 2000}, {20, 30}}) {
            BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
            BufferedImage preview = ThumbnailRenderer.render(image);
            assertTrue(preview.getWidth() > 0 && preview.getWidth() <= 432);
            assertTrue(preview.getHeight() > 0 && preview.getHeight() <= 576);
            assertTrue(preview.getWidth() <= size[0] && preview.getHeight() <= size[1]);
            assertNotSame(image, preview);
        }
    }
}
