package org.dustyroom.ui.loading;

record QueuedTask(TaskPriority priority, long sequence, Runnable task) {
}
