package org.dustyroom.ui.loading;

import org.dustyroom.be.animation.AnimationClip;

public record AnimatedPage(AnimationClip clip, int x, int y, int width, int height, boolean spread) {
}
