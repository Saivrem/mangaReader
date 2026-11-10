package org.dustyroom.ui.listeners;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MenuOpenListener implements MenuListener {
    private final Runnable callback;

    @Override
    public void menuSelected(MenuEvent event) {
        callback.run();
    }

    @Override
    public void menuDeselected(MenuEvent event) {
    }

    @Override
    public void menuCanceled(MenuEvent event) {
    }
}
