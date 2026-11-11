package org.dustyroom.ui.loading;

import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Runs blocking work off the EDT, then delivers completion on the EDT.
 */
@RequiredArgsConstructor
public final class BackgroundTask<T> extends SwingWorker<T, Void> {
    private final BackgroundOperation<T> operation;
    private final Consumer<BackgroundTask<T>> completion;

    @Override
    protected T doInBackground() throws Exception {
        return operation.execute(this::isCancelled);
    }

    @Override
    protected void done() {
        completion.accept(this);
    }
}
