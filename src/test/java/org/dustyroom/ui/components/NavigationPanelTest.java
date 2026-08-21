package org.dustyroom.ui.components;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationPanelTest {

    @Test
    void exposesIconButtonsInNavigationOrderWithAccessibleLabels() throws Exception {
        Fixture fixture = fixture();
        List<Action> expectedActions = List.of(
                fixture.actions.prevVolume(), fixture.actions.firstImage(), fixture.actions.prevImage(),
                fixture.actions.openFile(), fixture.actions.nextImage(), fixture.actions.lastImage(),
                fixture.actions.nextVolume()
        );
        Set<String> componentNames = new HashSet<>();

        assertEquals("Page navigation", fixture.panel.getAccessibleContext().getAccessibleName());
        assertEquals(expectedActions.size(), fixture.panel.getComponentCount());

        for (int index = 0; index < expectedActions.size(); index++) {
            Action expectedAction = expectedActions.get(index);
            JButton button = (JButton) fixture.panel.getComponent(index);
            String actionName = expectedAction.getValue(Action.NAME).toString();
            String description = expectedAction.getValue(Action.SHORT_DESCRIPTION).toString();
            String id = expectedAction.getValue(Action.ACTION_COMMAND_KEY).toString();

            assertSame(expectedAction, button.getAction());
            assertNull(button.getText());
            assertNotNull(button.getIcon());
            assertEquals(description, button.getToolTipText());
            assertEquals("navigation." + id, button.getName());
            assertTrue(componentNames.add(button.getName()), "Duplicate component name");
            assertEquals(actionName, button.getAccessibleContext().getAccessibleName());
            assertEquals(description, button.getAccessibleContext().getAccessibleDescription());
            assertTrue(hasVisiblePixels(button), "Empty icon for " + id);
        }
    }

    @Test
    void previousAndNextPageArrowheadsPointInOppositeDirections() throws Exception {
        Fixture fixture = fixture();
        BufferedImage previous = render((JButton) fixture.panel.getComponent(2));
        BufferedImage next = render((JButton) fixture.panel.getComponent(4));

        assertOpaque(previous, 6, 11);  // left-facing tip
        assertOpaque(previous, 10, 7); // arrowhead base above the tip
        assertOpaque(previous, 10, 15);
        assertOpaque(next, 16, 11);     // right-facing tip
        assertOpaque(next, 12, 7);     // arrowhead base above the tip
        assertOpaque(next, 12, 15);
    }

    @Test
    void volumeIconsReserveSeparateArrowAndFolderRegions() throws Exception {
        Fixture fixture = fixture();
        BufferedImage previousVolume = render((JButton) fixture.panel.getComponent(0));
        BufferedImage openFile = render((JButton) fixture.panel.getComponent(3));
        BufferedImage nextVolume = render((JButton) fixture.panel.getComponent(6));

        assertEquals(36, previousVolume.getWidth());
        assertEquals(36, nextVolume.getWidth());
        assertEquals(22, openFile.getWidth());

        assertRegionHasVisiblePixels(previousVolume, 1, 12, "previous-volume arrow");
        assertTransparentColumns(previousVolume, 13, 14);
        assertRegionHasVisiblePixels(previousVolume, 15, 34, "previous-volume folder");
        assertOpaque(previousVolume, 3, 12);

        assertRegionHasVisiblePixels(nextVolume, 1, 20, "next-volume folder");
        assertTransparentColumns(nextVolume, 21, 22);
        assertRegionHasVisiblePixels(nextVolume, 23, 35, "next-volume arrow");
        assertOpaque(nextVolume, 33, 12);
    }

    private static Fixture fixture() throws Exception {
        return onEdt(() -> {
            TestViewerActions actions = new TestViewerActions();
            return new Fixture(new NavigationPanel(actions), actions);
        });
    }

    private static boolean hasVisiblePixels(JButton button) {
        BufferedImage image = render(button);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BufferedImage render(JButton button) {
        Icon icon = button.getIcon();
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(button, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void assertOpaque(BufferedImage image, int x, int y) {
        assertFalse((image.getRGB(x, y) >>> 24) == 0, "Expected icon pixel at " + x + "," + y);
    }

    private static void assertRegionHasVisiblePixels(BufferedImage image, int fromX, int toX, String region) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = fromX; x <= toX; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return;
                }
            }
        }
        throw new AssertionError("Expected visible pixels in " + region);
    }

    private static void assertTransparentColumns(BufferedImage image, int fromX, int toX) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = fromX; x <= toX; x++) {
                assertEquals(
                        0,
                        image.getRGB(x, y) >>> 24,
                        "Expected separation gap at " + x + "," + y
                );
            }
        }
    }

    private static <T> T onEdt(Supplier<T> supplier) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }

    private record Fixture(NavigationPanel panel, TestViewerActions actions) {
    }
}
