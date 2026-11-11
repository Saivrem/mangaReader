package org.dustyroom.ui.listeners;

import lombok.RequiredArgsConstructor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class MouseClickListener extends MouseAdapter {
    private final Consumer<MouseEvent> callback;

    @Override
    public void mouseClicked(MouseEvent event) {
        callback.accept(event);
    }
}
