package org.dustyroom.be.models.animation;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class AnimatedPage {
    private AnimationClip clip;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean spread;

    @Builder
    public AnimatedPage(AnimationClip clip, int x, int y, int width, int height, boolean spread) {
        this.clip = clip;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.spread = spread;
    }

}
