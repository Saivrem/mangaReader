package org.dustyroom.ui.loading;

import org.dustyroom.be.animation.AnimationDecoder;
import org.dustyroom.be.models.animation.AnimationClip;
import org.dustyroom.be.models.animation.AnimationFrame;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Two queued frames per visible page. All decoding and resource cleanup run off the EDT.
 */
public final class BufferedAnimation implements AutoCloseable {
    private final ArrayBlockingQueue<AnimationFrame> frames = new ArrayBlockingQueue<>(2);
    private final Semaphore capacity = new Semaphore(2);
    private final Thread worker;
    private volatile boolean closed, complete;
    private AnimationFrame current;
    private long deadline;
    private boolean buffering;

    public BufferedAnimation(AnimationClip source, Consumer<Exception> errorHandler) {
        worker = new Thread(() -> {
            try {
                int pass = 0;
                while (!closed) {
                    int count = 0, loops;
                    try (AnimationDecoder decoder = source.getOpener().open()) {
                        while (!closed) {
                            // Reserve room before decoding, so a full buffer never triggers more I/O.
                            capacity.acquire();
                            if (closed) {
                                return;
                            }
                            AnimationFrame frame = decoder.next();
                            if (frame == null) {
                                capacity.release();
                                break;
                            }
                            if (closed) {
                                return;
                            }
                            frames.put(frame);
                            count++;
                        }
                        loops = decoder.loops();
                    }
                    pass++;
                    if (count <= 1 || (loops > 0 && pass >= loops)) {
                        break;
                    }
                }
            } catch (InterruptedException cancelled) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                if (!closed) {
                    errorHandler.accept(error);
                }
            } finally {
                complete = true;
                if (closed) {
                    frames.clear();
                }
            }
        }, "yonaka-gif-decoder");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Called only by the UI thread. Late ticks catch up with the timeline; an underrun holds the current frame without blocking.
     */
    public AnimationFrame advance(long nowNanos) {
        if (closed) {
            return null;
        }
        // Keep an absolute media timeline: late timer ticks must not stretch every frame.
        // Bound catch-up work to the queue capacity so the EDT always remains responsive.
        for (int consumed = 0; consumed < 2 && (current == null || nowNanos >= deadline); consumed++) {
            AnimationFrame next = frames.poll();
            if (next == null) {
                buffering = true;
                break;
            }
            capacity.release();
            long start = current == null || buffering ? nowNanos : deadline;
            current = next;
            deadline = start + TimeUnit.MILLISECONDS.toNanos(next.getDelayMillis());
            buffering = false;
        }
        return current;
    }

    public AnimationFrame currentFrame() {
        return current;
    }

    public boolean finished(long nowNanos) {
        return complete && frames.isEmpty() && (current == null || nowNanos >= deadline);
    }

    public int bufferedFrames() {
        return frames.size();
    }

    @Override
    public void close() {
        closed = true;
        worker.interrupt();
        frames.clear();
        current = null;
    }
}
