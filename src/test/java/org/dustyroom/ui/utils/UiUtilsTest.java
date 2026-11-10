package org.dustyroom.ui.utils;

import org.dustyroom.be.models.state.ViewerTheme;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class UiUtilsTest {
    @Test
    void switchesBothThemesAndUpdatesExistingComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel previous = UIManager.getLookAndFeel();
            boolean frameDecorated = JFrame.isDefaultLookAndFeelDecorated();
            boolean dialogDecorated = JDialog.isDefaultLookAndFeelDecorated();
            ViewerTheme previousTheme = UiUtils.currentTheme();
            try {
                assertTrue(UiUtils.installTheme(ViewerTheme.DARK));
                JPanel panel = new JPanel();
                assertEquals(new Color(0x0D1017), panel.getBackground());
                assertEquals("Yonaka Reader Ayu Dark", UIManager.getLookAndFeel().getName());
                assertTrue(UiUtils.installTheme(ViewerTheme.LIGHT));
                SwingUtilities.updateComponentTreeUI(panel);
                assertEquals(new Color(0xF8F9FA), panel.getBackground());
                assertEquals(new Color(0xF8F9FA), UIManager.getColor("Viewer.background"));
                assertEquals(ViewerTheme.LIGHT, UiUtils.currentTheme());
                assertFalse(JFrame.isDefaultLookAndFeelDecorated());
                assertFalse(JDialog.isDefaultLookAndFeelDecorated());
                assertTrue(UiUtils.installTheme(ViewerTheme.DARK));
                SwingUtilities.updateComponentTreeUI(panel);
                assertEquals(new Color(0x0D1017), panel.getBackground());
            } finally {
                UiUtils.installTheme(previousTheme);
                try {
                    UIManager.setLookAndFeel(previous);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
                JFrame.setDefaultLookAndFeelDecorated(frameDecorated);
                JDialog.setDefaultLookAndFeelDecorated(dialogDecorated);
            }
        });
    }
}
