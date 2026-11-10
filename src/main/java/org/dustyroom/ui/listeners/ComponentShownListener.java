package org.dustyroom.ui.listeners;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ComponentShownListener extends ComponentAdapter {
    private final Runnable callback;

    @Override
    public void componentShown(ComponentEvent event) {
        callback.run();
    }
}
