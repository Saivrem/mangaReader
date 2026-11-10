package org.dustyroom.ui.loading;

import java.awt.image.BufferedImage;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
final class CacheEntry {
    private final BufferedImage image;
    private final long weight;

    @Builder
    public CacheEntry(BufferedImage image, long weight) {
        this.image = image;
        this.weight = weight;
    }

}
