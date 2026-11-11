package org.dustyroom.be.models.animation;

import lombok.*;
import lombok.experimental.Accessors;
import org.dustyroom.be.animation.AnimationOpener;

/**
 * A reopenable animation source; owns no open files or decoded frame collection.
 */
@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class AnimationClip {
    private AnimationOpener opener;

    @Builder
    public AnimationClip(AnimationOpener opener) {
        this.opener = opener;
    }

}
