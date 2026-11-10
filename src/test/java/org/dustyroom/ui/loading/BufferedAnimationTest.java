package org.dustyroom.ui.loading;

import org.dustyroom.be.animation.AnimationClip;
import org.dustyroom.be.animation.AnimationDecoder;
import org.dustyroom.be.animation.AnimationFrame;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class BufferedAnimationTest {
    private static AnimationFrame frame(int value) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, value);
        return new AnimationFrame(image, 100);
    }

    private static void await(BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            while (!condition.getAsBoolean()) Thread.sleep(1);
        });
    }

    @Test
    void lateTicksDoNotAccumulateDelayAndPaintingDoesNotAdvancePlayback() {
        AnimationClip clip = new AnimationClip(() -> new AnimationDecoder() {
            private int index;
            public AnimationFrame next() { return frame(++index); }
            public int loops() { return 0; }
            public void close() { }
        });
        try (BufferedAnimation player = new BufferedAnimation(clip, error -> fail(error))) {
            await(() -> player.bufferedFrames() == 2);
            assertEquals(1, player.advance(0).image().getRGB(0, 0) & 255);
            await(() -> player.bufferedFrames() == 2);
            assertEquals(2, player.advance(TimeUnit.MILLISECONDS.toNanos(120)).image().getRGB(0, 0) & 255);
            await(() -> player.bufferedFrames() == 2);
            // Second deadline remains 200 ms, rather than drifting to 220 ms.
            assertEquals(3, player.advance(TimeUnit.MILLISECONDS.toNanos(200)).image().getRGB(0, 0) & 255);
            assertSame(player.currentFrame(), player.currentFrame());
            await(() -> player.bufferedFrames() == 2);
            assertEquals(5, player.advance(TimeUnit.MILLISECONDS.toNanos(450)).image().getRGB(0, 0) & 255);
        }
    }

    @Test
    void boundsReadAheadAndClosesBlockedProducerOnCancellation() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        CountDownLatch closed = new CountDownLatch(1);
        AnimationClip clip = new AnimationClip(() -> new AnimationDecoder() {
            public AnimationFrame next() {
                reads.incrementAndGet();
                return frame(1);
            }

            public int loops() {
                return 0;
            }

            public void close() {
                closed.countDown();
            }
        });
        try (BufferedAnimation player = new BufferedAnimation(clip, error -> fail(error))) {
            await(() -> player.bufferedFrames() == 2);
            assertEquals(2, reads.get());
            assertEquals(1, player.advance(0).image().getRGB(0, 0) & 255);
            await(() -> reads.get() == 3);
            assertNotNull(player.advance(1)); // Before deadline: no consumption.
            assertEquals(3, reads.get());
            player.close();
            assertTrue(closed.await(2, TimeUnit.SECONDS));
            assertNull(player.advance(Long.MAX_VALUE));
            assertEquals(0, player.bufferedFrames());
        }
    }

    @Test
    void reopensForFiniteLoopsAndRetainsFinalFrame() throws Exception {
        AtomicInteger opens = new AtomicInteger(), closes = new AtomicInteger();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AnimationClip clip = new AnimationClip(() -> {
            opens.incrementAndGet();
            return new AnimationDecoder() {
                int index;

                public AnimationFrame next() {
                    return index == 3 ? null : frame(++index);
                }

                public int loops() {
                    return 3;
                }

                public void close() {
                    closes.incrementAndGet();
                }
            };
        });
        try (BufferedAnimation player = new BufferedAnimation(clip, failure::set)) {
            for (int i = 0; i < 9; i++) {
                await(() -> player.bufferedFrames() > 0);
                AnimationFrame frame = player.advance(TimeUnit.MILLISECONDS.toNanos(i * 100L));
                assertEquals(i % 3 + 1, frame.image().getRGB(0, 0) & 255);
            }
            await(() -> player.finished(TimeUnit.SECONDS.toNanos(1)));
            assertEquals(3, opens.get());
            assertEquals(3, closes.get());
            assertEquals(3, player.advance(TimeUnit.SECONDS.toNanos(2)).image().getRGB(0, 0) & 255);
            assertNull(failure.get());
        }
    }

    @Test
    void decodeFailureClosesSourceAndReportsOnce() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        AnimationClip clip = new AnimationClip(() -> new AnimationDecoder() {
            public AnimationFrame next() throws IOException {
                throw new IOException("broken GIF");
            }

            public int loops() {
                return 0;
            }

            public void close() {
                closed.countDown();
            }
        });
        try (BufferedAnimation player = new BufferedAnimation(clip, failure::set)) {
            assertTrue(closed.await(2, TimeUnit.SECONDS));
            await(() -> player.finished(0));
            assertEquals("broken GIF", failure.get().getMessage());
            assertNull(player.advance(0));
        }
    }

    @Test
    void cancellationDuringDecodingDiscardsThePendingFrameAndClosesSource() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), closed = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();
        AnimationClip clip = new AnimationClip(() -> new AnimationDecoder() {
            public AnimationFrame next() throws IOException {
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException cancelled) {
                    throw new InterruptedIOException();
                }
                return frame(1);
            }

            public int loops() {
                return 0;
            }

            public void close() {
                closed.countDown();
            }
        });
        try (BufferedAnimation player = new BufferedAnimation(clip, failure::set)) {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            player.close();
            assertTrue(closed.await(2, TimeUnit.SECONDS));
            assertNull(player.advance(Long.MAX_VALUE));
            assertNull(failure.get());
        }
    }
}
