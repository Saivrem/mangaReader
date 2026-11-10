package org.dustyroom.ui.loading;

import java.util.function.Consumer;
import javax.swing.SwingWorker;
import lombok.RequiredArgsConstructor;

/** Runs blocking work off the EDT, then delivers completion on the EDT. */
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
