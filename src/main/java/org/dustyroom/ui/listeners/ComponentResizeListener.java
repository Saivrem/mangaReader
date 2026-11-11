package org.dustyroom.ui.listeners;

import lombok.RequiredArgsConstructor;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

@RequiredArgsConstructor
public final class ComponentResizeListener extends ComponentAdapter {
    private final Runnable callback;

    @Override
    public void componentResized(ComponentEvent event) {
        callback.run();
    }
}
