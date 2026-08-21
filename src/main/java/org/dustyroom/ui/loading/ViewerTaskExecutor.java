package org.dustyroom.ui.loading;

public interface ViewerTaskExecutor extends AutoCloseable {
    boolean execute(TaskPriority priority, Runnable task);

    @Override
    void close();
}
