package org.dustyroom.be.models;

import java.awt.image.BufferedImage;
import java.util.List;

/** Fully composited, bounded GIF frames decoded off the EDT. Zero loops means forever. */
public record AnimationClip(List<BufferedImage> frames, List<Integer> delays, int loops) {
    public AnimationClip {
        frames = List.copyOf(frames);
        delays = List.copyOf(delays);
        if (frames.isEmpty() || frames.size() != delays.size() || delays.stream().anyMatch(d -> d <= 0))
            throw new IllegalArgumentException("Invalid animation");
    }
    public boolean finished(long elapsedMillis) {
        return loops > 0 && elapsedMillis >= delays.stream().mapToLong(Integer::longValue).sum() * loops;
    }
    public int frameAt(long elapsedMillis) {
        long duration = delays.stream().mapToLong(Integer::longValue).sum();
        if (loops > 0 && elapsedMillis >= duration * loops) return frames.size() - 1;
        long position = Math.max(0, elapsedMillis) % duration;
        for (int i = 0; i < delays.size(); i++) {
            if (position < delays.get(i)) return i;
            position -= delays.get(i);
        }
        return 0;
    }
}
