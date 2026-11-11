package org.dustyroom.be.models.animation;

import lombok.*;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class AnimationFrame {
    private BufferedImage image;
    private int delayMillis;

    @Builder
    public AnimationFrame(BufferedImage image, int delayMillis) {
        this.image = image;
        this.delayMillis = delayMillis;
    }

}
