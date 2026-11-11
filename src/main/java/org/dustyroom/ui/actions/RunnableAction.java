package org.dustyroom.ui.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;

public final class RunnableAction extends AbstractAction {
    private final Runnable callback;

    public RunnableAction(String name, Runnable callback) {
        super(name);
        this.callback = callback;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        callback.run();
    }
}
