package org.dustyroom.ui.settings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.settings.SystemThemeDetector;

public final class SystemThemeMonitor implements AutoCloseable {
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "yonaka-system-theme");
        thread.setDaemon(true);
        return thread;
    });
    private volatile ThemeMode mode;
    private volatile boolean closed;

    public SystemThemeMonitor(ThemeMode initial, Consumer<ViewerTheme> changed) {
        mode = initial;
        worker.scheduleWithFixedDelay(() -> {
            if (mode == ThemeMode.SYSTEM && !closed) {
                new SystemThemeDetector().detect().ifPresent(theme -> SwingUtilities.invokeLater(() -> {
                    if (mode == ThemeMode.SYSTEM && !closed) {
                        changed.accept(theme);
                    }
                }));
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void setMode(ThemeMode mode) {
        this.mode = mode;
    }

    @Override
    public void close() {
        closed = true;
        worker.shutdownNow();
    }
}
