package org.dustyroom.ui.loading;

import lombok.Builder;

import java.awt.image.BufferedImage;

record CacheEntry(BufferedImage image, long weight) {
    @Builder
    CacheEntry {
    }

}
