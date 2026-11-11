package org.dustyroom.be.models.book;

import lombok.*;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class Picture {
    private BufferedImage image;
    private PictureMetadata metadata;

    @Builder
    public Picture(BufferedImage image, PictureMetadata metadata) {
        this.image = image;
        this.metadata = metadata;
    }

}
