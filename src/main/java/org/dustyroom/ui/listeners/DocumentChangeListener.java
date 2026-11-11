package org.dustyroom.ui.listeners;

import lombok.RequiredArgsConstructor;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@RequiredArgsConstructor
public final class DocumentChangeListener implements DocumentListener {
    private final Runnable callback;

    @Override
    public void insertUpdate(DocumentEvent event) {
        callback.run();
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        callback.run();
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        callback.run();
    }
}
