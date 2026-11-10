package org.dustyroom.ui.loading;

@FunctionalInterface
interface WorkerCommand {
    void run(long targetGeneration, long requestId);
}
