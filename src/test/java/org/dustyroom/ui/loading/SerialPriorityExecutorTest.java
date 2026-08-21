package org.dustyroom.ui.loading;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialPriorityExecutorTest {
    @Test
    void queuedInteractiveWorkRunsBeforePrefetch() throws Exception {
        SerialPriorityExecutor executor = new SerialPriorityExecutor();
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        List<String> order = new CopyOnWriteArrayList<>();

        executor.execute(TaskPriority.INTERACTIVE, () -> {
            blockerStarted.countDown();
            await(releaseBlocker);
        });
        assertTrue(blockerStarted.await(2, TimeUnit.SECONDS));

        executor.execute(TaskPriority.PREFETCH, () -> {
            order.add("prefetch");
            completed.countDown();
        });
        executor.execute(TaskPriority.INTERACTIVE, () -> {
            order.add("interactive");
            completed.countDown();
        });

        releaseBlocker.countDown();
        assertTrue(completed.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("interactive", "prefetch"), order);
        executor.close();
    }

    @Test
    void submissionAfterCloseIsRejectedWithoutThrowing() {
        SerialPriorityExecutor executor = new SerialPriorityExecutor();
        executor.close();

        assertFalse(executor.execute(TaskPriority.PREFETCH, () -> {
            throw new AssertionError("Rejected work must not run");
        }));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Timed out waiting for test latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
