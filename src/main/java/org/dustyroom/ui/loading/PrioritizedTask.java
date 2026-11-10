package org.dustyroom.ui.loading;

import java.util.concurrent.FutureTask;

final class PrioritizedTask extends FutureTask<Void> implements Comparable<PrioritizedTask> {
    private final TaskPriority priority;
    private final long sequence;

    PrioritizedTask(TaskPriority priority, long sequence, Runnable task) {
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
