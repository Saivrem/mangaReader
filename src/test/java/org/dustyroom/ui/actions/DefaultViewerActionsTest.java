package org.dustyroom.ui.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultViewerActionsTest {

    @Test
    void actionCarriesPresentationAndKeyboardMetadata() {
        AtomicBoolean invoked = new AtomicBoolean();
        KeyStroke accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);

        Action action = DefaultViewerActions.action(
                "openFile", "Open…", "Open a file", accelerator,
                () -> invoked.set(true));

        assertEquals("openFile", action.getValue(Action.ACTION_COMMAND_KEY));
        assertEquals("Open…", action.getValue(Action.NAME));
        assertEquals("Open a file", action.getValue(Action.SHORT_DESCRIPTION));
        Assertions.assertNull(action.getValue(Action.MNEMONIC_KEY));
        assertEquals(accelerator, action.getValue(Action.ACCELERATOR_KEY));

        action.actionPerformed(null);
        assertTrue(invoked.get());
    }
}
