package org.dustyroom.ui.loading;

import java.util.concurrent.*;
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

    private static final class PrioritizedTask extends FutureTask<Void> implements Comparable<PrioritizedTask> {
        private final TaskPriority priority;
        private final long sequence;

        private PrioritizedTask(TaskPriority priority, long sequence, Runnable task) {
            super(task, null);
            this.priority = priority;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(PrioritizedTask other) {
            int byPriority = Integer.compare(priority.ordinal(), other.priority.ordinal());
            return byPriority != 0 ? byPriority : Long.compare(sequence, other.sequence);
        }
    }
}
