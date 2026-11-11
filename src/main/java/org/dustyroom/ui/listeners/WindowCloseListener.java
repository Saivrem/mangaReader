package org.dustyroom.ui.listeners;

import lombok.RequiredArgsConstructor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@RequiredArgsConstructor
public final class WindowCloseListener extends WindowAdapter {
    private final Runnable callback;

    @Override
    public void windowClosing(WindowEvent event) {
        callback.run();
    }
}
