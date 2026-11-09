package org.dustyroom.ui.loading;

import org.dustyroom.be.models.AnimationClip;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Two queued frames per visible page. All decoding and resource cleanup run off the EDT. */
public final class BufferedAnimation implements AutoCloseable {
    private final ArrayBlockingQueue<AnimationClip.Frame> frames = new ArrayBlockingQueue<>(2);
    private final java.util.concurrent.Semaphore capacity = new java.util.concurrent.Semaphore(2);
    private final Thread worker;
    private volatile boolean closed, complete;
    private AnimationClip.Frame current;
    private long deadline;

    public BufferedAnimation(AnimationClip source, Consumer<Exception> errorHandler) {
        worker = new Thread(() -> {
            try {
                int pass = 0;
                while (!closed) {
                    int count = 0, loops;
                    try (var decoder = source.opener().open()) {
                        while (!closed) {
                            // Reserve room before decoding, so a full buffer never triggers more I/O.
                            capacity.acquire();
                            if (closed) return;
                            var frame = decoder.next();
                            if (frame == null) { capacity.release(); break; }
                            if (closed) return;
                            frames.put(frame);
                            count++;
                        }
                        loops = decoder.loops();
                    }
                    pass++;
                    if (count <= 1 || (loops > 0 && pass >= loops)) break;
                }
            } catch (InterruptedException cancelled) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                if (!closed) errorHandler.accept(error);
            } finally {
                complete = true;
                if (closed) frames.clear();
            }
        }, "yonaka-gif-decoder");
        worker.setDaemon(true);
        worker.start();
    }

    /** Called only by the UI thread. An underrun holds the current frame, never blocks or skips. */
    public AnimationClip.Frame advance(long nowNanos) {
        if (closed) return null;
        if (current == null || nowNanos >= deadline) {
            var next = frames.poll();
            if (next != null) {
                capacity.release();
                current = next;
                deadline = nowNanos + TimeUnit.MILLISECONDS.toNanos(next.delayMillis());
            }
        }
        return current;
    }

    public boolean finished(long nowNanos) {
        return complete && frames.isEmpty() && (current == null || nowNanos >= deadline);
    }

    public int bufferedFrames() { return frames.size(); }

    @Override public void close() {
        closed = true;
        worker.interrupt();
        frames.clear();
        current = null;
    }
}
