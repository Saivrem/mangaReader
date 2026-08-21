package org.dustyroom.ui.loading;

import org.dustyroom.be.models.PageKey;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BoundedImageCacheTest {
    private static final long LARGE_LIMIT = 1024 * 1024;

    @Test
    void evictsLeastRecentlyUsedEntry() {
        BoundedImageCache cache = new BoundedImageCache(2, LARGE_LIMIT);
        PageKey first = key("first");
        PageKey second = key("second");
        PageKey third = key("third");

        cache.put(first, image(2, 2));
        cache.put(second, image(2, 2));
        assertNotNull(cache.get(first));
        cache.put(third, image(2, 2));

        assertTrue(cache.contains(first));
        assertFalse(cache.contains(second));
        assertTrue(cache.contains(third));
    }

    @Test
    void enforcesEstimatedByteLimit() {
        BoundedImageCache cache = new BoundedImageCache(10, 20);
        PageKey first = key("first");
        PageKey second = key("second");

        cache.put(first, image(2, 2));
        cache.put(second, image(2, 2));

        assertFalse(cache.contains(first));
        assertTrue(cache.contains(second));
        assertEquals(16, cache.estimatedBytes());
    }

    @Test
    void replacementUpdatesWeightAndClearResetsState() {
        BoundedImageCache cache = new BoundedImageCache(3, LARGE_LIMIT);
        PageKey key = key("page");

        cache.put(key, image(2, 2));
        cache.put(key, image(3, 2));

        assertEquals(1, cache.size());
        assertEquals(24, cache.estimatedBytes());

        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(0, cache.estimatedBytes());
        assertNull(cache.get(key));
    }

    @Test
    void doesNotRetainAnImageLargerThanTheWholeBudget() {
        BoundedImageCache cache = new BoundedImageCache(3, 8);
        PageKey key = key("oversized");

        cache.put(key, image(2, 2));

        assertFalse(cache.contains(key));
        assertEquals(0, cache.estimatedBytes());
    }

    @Test
    void oversizedImageDoesNotEvictExistingEntries() {
        BoundedImageCache cache = new BoundedImageCache(3, 20);
        PageKey retained = key("retained");
        PageKey oversized = key("oversized");

        cache.put(retained, image(2, 2));
        cache.put(oversized, image(3, 3));

        assertTrue(cache.contains(retained));
        assertFalse(cache.contains(oversized));
        assertEquals(1, cache.size());
        assertEquals(16, cache.estimatedBytes());
    }

    private static PageKey key(String page) {
        return new PageKey("source", page);
    }

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
