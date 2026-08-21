package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class KeyBindingsTest {

    @Test
    void bindsKeyboardAndNumpadZoomVariants() throws Exception {
        Fixture fixture = fixture();

        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), fixture.actions.zoomIn());
        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), fixture.actions.zoomIn());
        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK), fixture.actions.zoomIn());
        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), fixture.actions.zoomIn());
        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), fixture.actions.zoomOut());
        assertBinding(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), fixture.actions.zoomOut());
    }

    @Test
    void escapeDelegatesToFullscreenEscapeWithoutExiting() throws Exception {
        Fixture fixture = fixture();
        Action action = boundAction(fixture, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        assertSame(fixture.actions.escapeFullscreen(), action);
        action.actionPerformed(new ActionEvent(fixture.rootPane, ActionEvent.ACTION_PERFORMED, "escape"));

        assertEquals(1, fixture.actions.invocationCount("escapeFullscreen"));
        assertEquals(0, fixture.actions.invocationCount("exit"));
    }

    @Test
    void bindsCanonicalPlatformOpenAndExitAccelerators() throws Exception {
        Fixture fixture = fixture();
        KeyStroke open = (KeyStroke) fixture.actions.openFile().getValue(Action.ACCELERATOR_KEY);
        KeyStroke exit = (KeyStroke) fixture.actions.exit().getValue(Action.ACCELERATOR_KEY);

        assertBinding(fixture, open, fixture.actions.openFile());
        assertBinding(fixture, exit, fixture.actions.exit());
    }

    private static Fixture fixture() throws Exception {
        return onEdt(() -> {
            JRootPane rootPane = new JRootPane();
            TestViewerActions actions = new TestViewerActions();
            new KeyBindings().init(rootPane, actions);
            return new Fixture(rootPane, actions);
        });
    }

    private static void assertBinding(Fixture fixture, KeyStroke keyStroke, Action expected) {
        assertSame(expected, boundAction(fixture, keyStroke), () -> "Wrong action for " + keyStroke);
    }

    private static Action boundAction(Fixture fixture, KeyStroke keyStroke) {
        InputMap inputMap = fixture.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = fixture.rootPane.getActionMap();
        return actionMap.get(inputMap.get(keyStroke));
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }

    private record Fixture(JRootPane rootPane, TestViewerActions actions) {
    }
}
