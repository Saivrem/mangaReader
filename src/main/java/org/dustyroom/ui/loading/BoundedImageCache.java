package org.dustyroom.ui.loading;

import org.dustyroom.be.models.PageKey;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoundedImageCache {
    private final int maxEntries;
    private final long maxBytes;
    private final Map<PageKey, CacheEntry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long estimatedBytes;

    public BoundedImageCache(int maxEntries, long maxBytes) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.maxEntries = maxEntries;
        this.maxBytes = maxBytes;
    }

    public synchronized BufferedImage get(PageKey key) {
        CacheEntry entry = entries.get(key);
        return entry == null ? null : entry.image();
    }

    public synchronized void put(PageKey key, BufferedImage image) {
        long weight = estimatedSize(image);
        if (weight > maxBytes) {
            return;
        }
        CacheEntry previous = entries.put(key, new CacheEntry(image, weight));
        if (previous != null) {
            estimatedBytes -= previous.weight();
        }
        estimatedBytes = saturatedAdd(estimatedBytes, weight);
        evictIfNeeded();
    }

    public synchronized boolean contains(PageKey key) {
        return entries.containsKey(key);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized long estimatedBytes() {
        return estimatedBytes;
    }

    public synchronized void clear() {
        entries.clear();
        estimatedBytes = 0;
    }

    private void evictIfNeeded() {
        Iterator<Map.Entry<PageKey, CacheEntry>> iterator = entries.entrySet().iterator();
        while ((entries.size() > maxEntries || estimatedBytes > maxBytes) && iterator.hasNext()) {
            CacheEntry eldest = iterator.next().getValue();
            estimatedBytes -= eldest.weight();
            iterator.remove();
        }
    }

    private long estimatedSize(BufferedImage image) {
        long pixels = (long) image.getWidth() * image.getHeight();
        return pixels > Long.MAX_VALUE / 4 ? Long.MAX_VALUE : pixels * 4;
    }

    private long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private record CacheEntry(BufferedImage image, long weight) {
    }
}
