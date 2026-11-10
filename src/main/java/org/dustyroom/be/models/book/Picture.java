package org.dustyroom.be.models.book;

import java.awt.image.BufferedImage;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

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
