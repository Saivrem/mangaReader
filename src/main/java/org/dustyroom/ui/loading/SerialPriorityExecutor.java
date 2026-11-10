package org.dustyroom.ui.loading;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SerialPriorityExecutor implements ViewerTaskExecutor {
    private final AtomicLong sequence = new AtomicLong();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(),
            runnable -> {
        Thread thread = new Thread(runnable, "manga-reader-image-loader");
        thread.setDaemon(true);
        return thread;
    }
    );

    @Override
    public boolean execute(TaskPriority priority, Runnable task) {
        try {
            executor.execute(new PrioritizedTask(priority, sequence.getAndIncrement(), task));
            return true;
        } catch (RejectedExecutionException ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

}
