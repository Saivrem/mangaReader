package org.dustyroom.ui.components;

import org.dustyroom.persistence.ReaderSettings;
import org.dustyroom.ui.actions.ViewerActionState;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.ui.theme.ViewerTheme;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ReaderUiTest {
    private static Stream<Component> children(Container parent) {
        return Arrays.stream(parent.getComponents()).flatMap(component ->
                component instanceof Container container
                        ? Stream.concat(Stream.of(component), children(container)) : Stream.of(component));
    }

    private static ViewerActions actions() {
        Map<String, Action> actions = new HashMap<>();
        return (ViewerActions) Proxy.newProxyInstance(ViewerActions.class.getClassLoader(),
                new Class<?>[]{ViewerActions.class}, (proxy, method, arguments) ->
                        actions.computeIfAbsent(method.getName(), name -> new AbstractAction(name) {
                            @Override
                            public void actionPerformed(ActionEvent event) {
                            }
                        }));
    }

    @Test
    void toolbarTracksDisplayedPagesModesAndAvailability() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ViewerActions actions = actions();
            ViewerActionState.apply(actions, null, FitMode.FIT_SCREEN, false);
            NavigationPanel panel = new NavigationPanel(actions);
            assertFalse(actions.nextImage().isEnabled());
            assertFalse(actions.goToPage().isEnabled());
            assertTrue(actions.openFile().isEnabled());
            ViewerFrame frame = new ViewerFrame(null, "Book", null, 2, 3, 10, true, ReadingMode.COMICS, null);
            ViewerActionState.apply(actions, frame, FitMode.FIT_WIDTH, true);
            panel.updateState(frame, FitMode.FIT_WIDTH);
            assertEquals(Boolean.TRUE, actions.fitWidth().getValue(Action.SELECTED_KEY));
            assertEquals(Boolean.FALSE, actions.fitScreen().getValue(Action.SELECTED_KEY));
            assertEquals(Boolean.TRUE, actions.toggleTwoPageMode().getValue(Action.SELECTED_KEY));
            assertEquals(Boolean.TRUE, actions.setComicsReadingMode().getValue(Action.SELECTED_KEY));
            assertEquals(Boolean.FALSE, actions.setMangaReadingMode().getValue(Action.SELECTED_KEY));
            assertTrue(actions.exitFullscreen().isEnabled());
            assertTrue(children(panel).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .anyMatch(button -> "2–3 / 10".equals(button.getText())));
            children(panel).filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                    .forEach(button -> assertEquals(0, button.getMnemonic()));
            ViewerActionState.apply(actions, new ViewerFrame(null, "Book", null, 10, 10, 10, false,
                    ReadingMode.MANGA, null), FitMode.ORIGINAL, false);
            assertFalse(actions.lastImage().isEnabled());
            assertTrue(actions.nextImage().isEnabled()); // existing wrap-around navigation
            assertFalse(actions.exitFullscreen().isEnabled());
        });
    }

    @Test
    void welcomeOpensWithTheSharedActionAndGivesWayToReader() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ViewerActions actions = actions();
            JScrollPane reader = new JScrollPane();
            ReaderSurface surface = new ReaderSurface(reader, actions.openFile());
            assertFalse(reader.isVisible());
            assertTrue(children(surface).filter(JButton.class::isInstance).map(JButton.class::cast)
                    .anyMatch(button -> button.getAction() == actions.openFile()));
            surface.setLoading(true);
            assertTrue(children(surface).filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> "Opening your book…".equals(label.getText())));
            surface.showReading();
            assertTrue(reader.isVisible());
        });
    }

    @Test
    void preservesCustomZoomAcrossPagesAndKeepsItWithinTheStoredRange() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImageComponent component = new ImageComponent();
            JScrollPane scroll = new JScrollPane(component);
            scroll.getViewport().setSize(800, 600);
            component.restoreView(FitMode.ORIGINAL, 1.75);
            component.setImageAndCenter(new BufferedImage(4, 8, BufferedImage.TYPE_INT_RGB), scroll);
            assertEquals(1.75, component.getScale());
            component.setImageAndCenter(new BufferedImage(8, 16, BufferedImage.TYPE_INT_RGB), scroll);
            assertEquals(1.75, component.getScale());
            component.setFitMode(FitMode.FIT_SCREEN);
            component.zoomOut();
            assertTrue(component.getScale() <= 20);
            assertDoesNotThrow(() -> new ReaderSettings(
                    ViewerTheme.DARK, component.getFitMode(), component.getScale()));
        });
    }

    @Test
    void imageSurfacePaintsTheCurrentThemeBackgroundEvenWithoutAnImage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImageComponent component = new ImageComponent();
            component.setOpaque(true);
            component.setSize(40, 40);
            BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            for (Color color : new Color[]{new Color(0x0D1017), new Color(0xF8F9FA)}) {
                component.setBackground(color);
                Graphics2D graphics = canvas.createGraphics();
                try {
                    component.paint(graphics);
                } finally {
                    graphics.dispose();
                }
                assertEquals(color.getRGB(), canvas.getRGB(20, 20));
            }
        });
    }
}
