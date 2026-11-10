package org.dustyroom.be.models.animation;

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
public final class AnimationFrame {
    private BufferedImage image;
    private int delayMillis;

    @Builder
    public AnimationFrame(BufferedImage image, int delayMillis) {
        this.image = image;
        this.delayMillis = delayMillis;
    }

}
