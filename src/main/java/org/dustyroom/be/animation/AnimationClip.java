package org.dustyroom.be.animation;

/**
 * A reopenable animation source; owns no open files or decoded frame collection.
 */
public record AnimationClip(AnimationOpener opener) {
}
