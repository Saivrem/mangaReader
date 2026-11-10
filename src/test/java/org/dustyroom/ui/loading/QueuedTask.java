package org.dustyroom.ui.loading;

import lombok.Value;

@Value
class QueuedTask {
    TaskPriority priority;
    long sequence;
    Runnable task;
}
