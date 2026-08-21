package org.dustyroom.ui.window;

import org.dustyroom.ui.LookSettings;
import org.dustyroom.ui.theme.ThemeManager;
import org.dustyroom.ui.window.platform.ChromeSpec;
import org.dustyroom.ui.window.platform.DesktopPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("Swing UIManager defaults")
class WindowComponentsTest {
    private final Map<String, SavedDefault> savedDefaults = new HashMap<>();

    @AfterEach
    void restoreUiDefaults() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        savedDefaults.forEach((key, saved) -> {
            if (saved.present()) {
                defaults.put(key, saved.value());
            } else {
                defaults.remove(key);
            }
        });
    }

    @Test
    void titleBarUsesThemeKeysAndTracksTitleAndFocus() throws Exception {
        setLafDefault("MangaReader.titleBarBackground", new Color(20, 30, 40));
        setLafDefault("MangaReader.titleBarForeground", new Color(220, 225, 230));
        Color inactiveBackground = new Color(241, 243, 246);
        Color inactiveForeground = new Color(98, 106, 120);
        setLafDefault("MangaReader.titleBarInactiveBackground", inactiveBackground);
        setLafDefault("MangaReader.titleBarInactiveForeground", inactiveForeground);
        AtomicReference<TitleBar> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TitleBar titleBar = new TitleBar(noopCommands());
            titleBar.setTitle("Chapter 12");
            reference.set(titleBar);
        });

        TitleBar titleBar = reference.get();
        assertEquals("Chapter 12", titleBar.title());
        assertEquals(new Color(20, 30, 40), titleBar.getBackground());
        assertTrue(titleBar.isWindowActive());

        SwingUtilities.invokeAndWait(() -> titleBar.setWindowActive(false));
        assertFalse(titleBar.isWindowActive());
        assertEquals(inactiveBackground, titleBar.getBackground());
        assertEquals(inactiveForeground, titleLabel(titleBar).getForeground());
    }

    @Test
    void titleBarAndControlsUseCompactFullHeightLayout() throws Exception {
        setLafDefault("MangaReader.titleBarHeight", 30);
        setLafDefault("MangaReader.titleBarButtonSize", new Dimension(44, 30));
        AtomicReference<TitleBar> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TitleBar titleBar = new TitleBar(noopCommands());
            titleBar.setSize(400, 30);
            titleBar.doLayout();
            Container controls = referenceControls(titleBar);
            controls.doLayout();
            reference.set(titleBar);
        });

        TitleBar titleBar = reference.get();
        assertEquals(30, titleBar.getPreferredSize().height);
        Container controls = referenceControls(titleBar);
        assertEquals(30, controls.getHeight());
        for (WindowControlButton button : titleBar.controlButtons()) {
            assertEquals(0, button.getY());
            assertEquals(30, button.getHeight());
        }
    }

    @Test
    void macOsTitleBarUsesLeftTrafficLightsAndCentersTitleAgainstTheWholeWindow() throws Exception {
        setLafDefault("MangaReader.titleBarHeight", 30);
        setLafDefault("MangaReader.macTitleBarButtonSize", new Dimension(20, 30));
        setLafDefault("MangaReader.macTitleBarControlsInset", 4);
        AtomicReference<TitleBar> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TitleBar titleBar = new TitleBar(
                    noopCommands(),
                    ChromeSpec.forPlatform(DesktopPlatform.MACOS)
            );
            titleBar.setTitle("Chapter 12");
            titleBar.setSize(400, 30);
            titleBar.doLayout();
            referenceControls(titleBar).doLayout();
            reference.set(titleBar);
        });

        TitleBar titleBar = reference.get();
        Container controls = referenceControls(titleBar);
        JLabel label = titleLabel(titleBar);
        Component balancingSpacer = Arrays.stream(titleBar.getComponents())
                .filter(component -> component != controls && component != label)
                .findFirst()
                .orElseThrow();

        assertEquals(0, controls.getX());
        assertEquals(
                List.of(
                        WindowControlButton.Control.CLOSE,
                        WindowControlButton.Control.MINIMIZE,
                        WindowControlButton.Control.MAXIMIZE
                ),
                visualControlOrder(controls)
        );
        assertEquals(titleBar.getWidth() / 2, label.getX() + label.getWidth() / 2);
        assertEquals(JLabel.CENTER, label.getHorizontalAlignment());
        assertEquals(controls.getWidth(), balancingSpacer.getWidth());
        assertTrue(titleBar.dragSurfaces().contains(controls));
        assertTrue(titleBar.dragSurfaces().contains(balancingSpacer));
        assertTrue(titleBar.controlButtons().stream().noneMatch(titleBar.dragSurfaces()::contains));

        WindowControlButton zoom = titleBar.controlButtons().get(1);
        assertEquals("Zoom", zoom.getAccessibleContext().getAccessibleName());
        SwingUtilities.invokeAndWait(() -> titleBar.setMaximized(true));
        assertEquals("Restore", zoom.getAccessibleContext().getAccessibleName());
        SwingUtilities.invokeAndWait(() -> titleBar.setMaximized(false));
        assertEquals("Zoom", zoom.getAccessibleContext().getAccessibleName());
    }

    @Test
    void windowsTitleBarUsesRightAlignedSystemControlOrder() throws Exception {
        setLafDefault("MangaReader.titleBarHeight", 30);
        setLafDefault("MangaReader.titleBarButtonSize", new Dimension(44, 30));
        AtomicReference<TitleBar> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TitleBar titleBar = new TitleBar(
                    noopCommands(),
                    ChromeSpec.forPlatform(DesktopPlatform.WINDOWS)
            );
            titleBar.setSize(400, 30);
            titleBar.doLayout();
            referenceControls(titleBar).doLayout();
            reference.set(titleBar);
        });

        TitleBar titleBar = reference.get();
        Container controls = referenceControls(titleBar);
        assertEquals(titleBar.getWidth(), controls.getX() + controls.getWidth());
        assertEquals(
                List.of(
                        WindowControlButton.Control.MINIMIZE,
                        WindowControlButton.Control.MAXIMIZE,
                        WindowControlButton.Control.CLOSE
                ),
                visualControlOrder(controls)
        );
        assertEquals(JLabel.LEADING, titleLabel(titleBar).getHorizontalAlignment());
        assertEquals(2, titleBar.getComponentCount());
        assertTrue(titleBar.dragSurfaces().contains(controls));
        assertTrue(titleBar.controlButtons().stream().noneMatch(titleBar.dragSurfaces()::contains));
    }

    @Test
    void explicitChromeSpecFlowsThroughTheWindowShell() throws Exception {
        ChromeSpec spec = ChromeSpec.forPlatform(DesktopPlatform.WINDOWS);
        AtomicReference<WindowChrome> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> reference.set(new WindowChrome(
                new JPanel(),
                null,
                null,
                spec
        )));

        assertEquals(spec, reference.get().titleBar().chromeSpec());
    }

    @Test
    void existingPlatformChromeSurvivesDarkLightDarkThemeSwitches() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                assertTrue(ThemeManager.install(LookSettings.MANGA_READER_DARK));
                ChromeSpec spec = ChromeSpec.forPlatform(DesktopPlatform.MACOS);
                WindowChrome chrome = new WindowChrome(new JPanel(), null, null, spec);
                layoutTitleBar(chrome.titleBar(), 400, 30);
                Rectangle darkControls = referenceControls(chrome.titleBar()).getBounds();
                assertEquals(new Color(0x20242D), chrome.titleBar().getBackground());

                assertTrue(ThemeManager.applyToComponentTrees(LookSettings.MANGA_READER_LIGHT, chrome));
                layoutTitleBar(chrome.titleBar(), 400, 30);
                assertEquals(spec, chrome.titleBar().chromeSpec());
                assertEquals(darkControls, referenceControls(chrome.titleBar()).getBounds());
                assertEquals(new Color(0xFFFFFF), chrome.titleBar().getBackground());

                chrome.titleBar().setWindowActive(false);
                assertEquals(new Color(0xF1F3F6), chrome.titleBar().getBackground());
                assertEquals(new Color(0x626A78), titleLabel(chrome.titleBar()).getForeground());

                assertTrue(ThemeManager.applyToComponentTrees(LookSettings.MANGA_READER_DARK, chrome));
                chrome.titleBar().setWindowActive(true);
                layoutTitleBar(chrome.titleBar(), 400, 30);
                assertEquals(spec, chrome.titleBar().chromeSpec());
                assertEquals(darkControls, referenceControls(chrome.titleBar()).getBounds());
                assertEquals(new Color(0x20242D), chrome.titleBar().getBackground());
            } finally {
                assertTrue(ThemeManager.installDefault());
            }
        });
    }

    @Test
    void windowControlIsAccessibleFocusableAndKeyboardActivatable() throws Exception {
        AtomicInteger activations = new AtomicInteger();
        AtomicReference<WindowControlButton> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> reference.set(new WindowControlButton(
                WindowControlButton.Control.CLOSE,
                activations::incrementAndGet
        )));

        WindowControlButton button = reference.get();
        assertTrue(button.isFocusable());
        assertEquals("Close", button.getAccessibleContext().getAccessibleName());
        assertEquals("Close", button.getAccessibleContext().getAccessibleDescription());
        assertNotNull(button.getActionMap().get("activateWindowControl"));

        SwingUtilities.invokeAndWait(() -> {
            invokeFocusedKey(button, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            assertEquals(1, activations.get());

            KeyStroke spacePressed = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false);
            invokeFocusedKey(button, spacePressed);
            invokeFocusedKey(button, spacePressed);
            assertEquals(1, activations.get());

            invokeFocusedKey(button, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
        });
        assertEquals(2, activations.get());
    }

    @Test
    void macOsTrafficLightsUseDistinctCircularActiveAndInactiveColors() throws Exception {
        setLafDefault("MangaReader.macTitleBarButtonSize", new Dimension(20, 30));
        Color close = new Color(240, 70, 65);
        Color minimize = new Color(245, 180, 50);
        Color zoom = new Color(45, 195, 75);
        Color inactive = new Color(105, 108, 116);
        setLafDefault("MangaReader.macTrafficLightClose", close);
        setLafDefault("MangaReader.macTrafficLightMinimize", minimize);
        setLafDefault("MangaReader.macTrafficLightZoom", zoom);
        setLafDefault("MangaReader.macTrafficLightInactive", inactive);
        AtomicReference<Map<WindowControlButton.Control, BufferedImage>> activeImages = new AtomicReference<>();
        AtomicReference<BufferedImage> inactiveImage = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            Map<WindowControlButton.Control, BufferedImage> rendered =
                    new EnumMap<>(WindowControlButton.Control.class);
            for (WindowControlButton.Control control : WindowControlButton.Control.values()) {
                WindowControlButton button = new WindowControlButton(
                        control,
                        () -> { },
                        ChromeSpec.forPlatform(DesktopPlatform.MACOS)
                );
                rendered.put(control, render(button));
                if (control == WindowControlButton.Control.CLOSE) {
                    button.setWindowActive(false);
                    inactiveImage.set(render(button));
                }
            }
            activeImages.set(rendered);
        });

        assertEquals(close, pixel(activeImages.get().get(WindowControlButton.Control.CLOSE), 10, 15));
        assertEquals(minimize, pixel(activeImages.get().get(WindowControlButton.Control.MINIMIZE), 10, 15));
        assertEquals(zoom, pixel(activeImages.get().get(WindowControlButton.Control.MAXIMIZE), 10, 15));
        assertEquals(inactive, pixel(inactiveImage.get(), 10, 15));
        assertEquals(0, pixel(activeImages.get().get(WindowControlButton.Control.CLOSE), 0, 0).getAlpha());
    }

    @Test
    void macOsHoverShowsGlyphsAcrossTheWholeTrafficLightCluster() throws Exception {
        setLafDefault("MangaReader.macTitleBarButtonSize", new Dimension(20, 30));
        Map<WindowControlButton.Control, Color> baseColors = new EnumMap<>(WindowControlButton.Control.class);
        baseColors.put(WindowControlButton.Control.CLOSE, new Color(240, 70, 65));
        baseColors.put(WindowControlButton.Control.MINIMIZE, new Color(245, 180, 50));
        baseColors.put(WindowControlButton.Control.MAXIMIZE, new Color(45, 195, 75));
        setLafDefault("MangaReader.macTrafficLightClose", baseColors.get(WindowControlButton.Control.CLOSE));
        setLafDefault("MangaReader.macTrafficLightMinimize", baseColors.get(WindowControlButton.Control.MINIMIZE));
        setLafDefault("MangaReader.macTrafficLightZoom", baseColors.get(WindowControlButton.Control.MAXIMIZE));
        AtomicReference<Map<WindowControlButton.Control, BufferedImage>> renderedImages = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TitleBar titleBar = new TitleBar(
                    noopCommands(),
                    ChromeSpec.forPlatform(DesktopPlatform.MACOS)
            );
            titleBar.setSize(400, 30);
            titleBar.doLayout();
            referenceControls(titleBar).doLayout();
            titleBar.controlButtons().stream()
                    .filter(button -> button.control() == WindowControlButton.Control.CLOSE)
                    .findFirst()
                    .orElseThrow()
                    .getModel()
                    .setRollover(true);
            Map<WindowControlButton.Control, BufferedImage> rendered =
                    new EnumMap<>(WindowControlButton.Control.class);
            titleBar.controlButtons().forEach(button -> rendered.put(button.control(), render(button)));
            renderedImages.set(rendered);
        });

        for (WindowControlButton.Control control : WindowControlButton.Control.values()) {
            BufferedImage image = renderedImages.get().get(control);
            int centerX = image.getWidth() / 2;
            int centerY = image.getHeight() / 2;
            assertNotEquals(baseColors.get(control), pixel(image, centerX, centerY));
        }
    }

    @Test
    void windowsCloseHoverAndPressedFeedbackFillTheWholeButton() throws Exception {
        setLafDefault("MangaReader.titleBarButtonSize", new Dimension(44, 30));
        Color hover = new Color(190, 45, 35);
        Color pressed = new Color(150, 35, 30);
        setLafDefault("MangaReader.titleBarCloseHoverBackground", hover);
        setLafDefault("MangaReader.titleBarClosePressedBackground", pressed);
        AtomicReference<BufferedImage> hoverImage = new AtomicReference<>();
        AtomicReference<BufferedImage> pressedImage = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            WindowControlButton button = new WindowControlButton(
                    WindowControlButton.Control.CLOSE,
                    () -> { },
                    ChromeSpec.forPlatform(DesktopPlatform.WINDOWS)
            );
            button.getModel().setRollover(true);
            hoverImage.set(render(button));
            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            pressedImage.set(render(button));
        });

        assertEquals(hover, pixel(hoverImage.get(), 1, 1));
        assertEquals(hover, pixel(hoverImage.get(), 1, 29));
        assertEquals(pressed, pixel(pressedImage.get(), 1, 1));
        assertEquals(pressed, pixel(pressedImage.get(), 1, 29));
    }

    @Test
    void neutralFallbackDoesNotBorrowWindowsDestructiveCloseFeedback() throws Exception {
        setLafDefault("MangaReader.titleBarButtonSize", new Dimension(44, 30));
        Color neutralHover = new Color(55, 65, 80);
        setLafDefault("MangaReader.titleBarButtonHoverBackground", neutralHover);
        setLafDefault("MangaReader.titleBarCloseHoverBackground", new Color(190, 45, 35));
        AtomicReference<BufferedImage> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            WindowControlButton button = new WindowControlButton(
                    WindowControlButton.Control.CLOSE,
                    () -> { },
                    ChromeSpec.forPlatform(DesktopPlatform.LINUX)
            );
            button.getModel().setRollover(true);
            reference.set(render(button));
        });

        assertEquals(neutralHover, pixel(reference.get(), 1, 1));
        assertEquals(neutralHover, pixel(reference.get(), 1, 29));
    }

    @Test
    void titleBarControlsDelegateAllWindowCommands() throws Exception {
        AtomicInteger minimize = new AtomicInteger();
        AtomicInteger maximize = new AtomicInteger();
        AtomicInteger close = new AtomicInteger();
        AtomicReference<TitleBar> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> reference.set(new TitleBar(new TitleBar.WindowCommands() {
            @Override
            public void minimize() {
                minimize.incrementAndGet();
            }

            @Override
            public void toggleMaximized() {
                maximize.incrementAndGet();
            }

            @Override
            public void requestClose() {
                close.incrementAndGet();
            }
        })));

        SwingUtilities.invokeAndWait(() -> reference.get().controlButtons().get(0).doClick(0));
        assertEquals(1, minimize.get());
        assertEquals(0, maximize.get());
        assertEquals(0, close.get());

        SwingUtilities.invokeAndWait(() -> reference.get().controlButtons().get(1).doClick(0));
        assertEquals(1, minimize.get());
        assertEquals(1, maximize.get());
        assertEquals(0, close.get());

        SwingUtilities.invokeAndWait(() -> reference.get().controlButtons().get(2).doClick(0));
        assertEquals(1, minimize.get());
        assertEquals(1, maximize.get());
        assertEquals(1, close.get());
    }

    @Test
    void maximizeControlChangesToRestoreSemantics() throws Exception {
        AtomicReference<WindowControlButton> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new WindowControlButton(
                WindowControlButton.Control.MAXIMIZE,
                () -> { }
        )));

        SwingUtilities.invokeAndWait(() -> reference.get().setMaximized(true));

        assertTrue(reference.get().isMaximized());
        assertEquals("Restore", reference.get().getAccessibleContext().getAccessibleName());
    }

    @Test
    void fullscreenChromeHidesAndRestoresSurfacesAndResizeHandles() throws Exception {
        AtomicReference<WindowChrome> reference = new AtomicReference<>();
        JMenuBar menuBar = new JMenuBar();
        JPanel footer = new JPanel();

        SwingUtilities.invokeAndWait(() -> reference.set(new WindowChrome(new JPanel(), menuBar, footer)));
        WindowChrome chrome = reference.get();
        assertTrue(chrome.resizeHandles().stream().allMatch(handle -> handle.isVisible()));

        SwingUtilities.invokeAndWait(() -> chrome.setFullscreen(true));
        assertFalse(chrome.titleBar().isVisible());
        assertFalse(menuBar.isVisible());
        assertFalse(footer.isVisible());
        assertTrue(chrome.resizeHandles().stream().noneMatch(handle -> handle.isVisible()));

        SwingUtilities.invokeAndWait(() -> chrome.setFullscreen(false));
        assertTrue(chrome.titleBar().isVisible());
        assertTrue(menuBar.isVisible());
        assertTrue(footer.isVisible());
        assertTrue(chrome.resizeHandles().stream().allMatch(handle -> handle.isVisible()));
    }

    @Test
    void maximizedChromeDisablesResizeButKeepsTitleVisible() throws Exception {
        AtomicReference<WindowChrome> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new WindowChrome(new JPanel(), null, null)));

        SwingUtilities.invokeAndWait(() -> reference.get().setMaximized(true));

        assertTrue(reference.get().titleBar().isVisible());
        assertTrue(reference.get().resizeHandles().stream().noneMatch(handle -> handle.isVisible()));
    }

    @Test
    void maximizedChromeRestoresItsStateAfterFullscreen() throws Exception {
        AtomicReference<WindowChrome> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new WindowChrome(
                new JPanel(),
                null,
                null,
                ChromeSpec.forPlatform(DesktopPlatform.MACOS)
        )));

        SwingUtilities.invokeAndWait(() -> {
            reference.get().setMaximized(true);
            reference.get().setFullscreen(true);
            reference.get().setFullscreen(false);
        });

        WindowChrome chrome = reference.get();
        assertTrue(chrome.isMaximized());
        assertTrue(chrome.titleBar().isVisible());
        assertTrue(chrome.resizeHandles().stream().noneMatch(handle -> handle.isVisible()));
        assertEquals(
                "Restore",
                chrome.titleBar().controlButtons().get(1).getAccessibleContext().getAccessibleName()
        );
    }

    @Test
    void resizeHandlesOverlayBodyWithoutConsumingOuterInsets() throws Exception {
        setLafDefault("MangaReader.titleBarHeight", 30);
        setLafDefault("MangaReader.titleBarButtonSize", new Dimension(44, 30));
        setLafDefault("MangaReader.windowResizeHandleThickness", 5);
        JPanel content = new JPanel();
        content.setPreferredSize(new Dimension(320, 240));
        AtomicReference<WindowChrome> reference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            WindowChrome chrome = new WindowChrome(content, null, null);
            chrome.setSize(400, 300);
            chrome.doLayout();
            reference.set(chrome);
        });

        WindowChrome chrome = reference.get();
        Component body = Arrays.stream(chrome.getComponents())
                .filter(component -> !(component instanceof WindowChrome.ResizeHandle))
                .findFirst()
                .orElseThrow();
        assertEquals(new Rectangle(0, 0, 400, 300), body.getBounds());
        assertEquals(new Insets(0, 0, 0, 0), chrome.getInsets());
        assertEquals(new Dimension(320, 270), chrome.getPreferredSize());
        assertEquals(
                new Rectangle(5, 0, 390, 5),
                handle(chrome, ResizeEdge.NORTH).getBounds()
        );
        assertEquals(
                new Rectangle(0, 0, 5, 5),
                handle(chrome, ResizeEdge.NORTH_WEST).getBounds()
        );
        assertEquals(
                new Rectangle(395, 295, 5, 5),
                handle(chrome, ResizeEdge.SOUTH_EAST).getBounds()
        );
    }

    @Test
    void existingChromeFallsBackToCurrentLookAndFeelAfterThemeDefaultsDisappear() throws Exception {
        Color customBackground = new Color(20, 30, 40);
        Color fallbackBackground = new Color(226, 228, 232);
        setLafDefault("MangaReader.titleBarBackground", customBackground);
        setLafDefault("MangaReader.titleBarForeground", new Color(230, 230, 230));
        AtomicReference<WindowChrome> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new WindowChrome(new JPanel(), null, null)));
        assertEquals(customBackground, reference.get().titleBar().getBackground());

        removeLafDefault("MangaReader.titleBarBackground");
        removeLafDefault("MangaReader.titleBarForeground");
        setLafDefault("MenuBar.background", fallbackBackground);
        setLafDefault("Label.foreground", new Color(25, 28, 32));
        SwingUtilities.invokeAndWait(() -> SwingUtilities.updateComponentTreeUI(reference.get()));

        assertEquals(fallbackBackground, reference.get().titleBar().getBackground());
    }

    private static TitleBar.WindowCommands noopCommands() {
        return new TitleBar.WindowCommands() {
            @Override
            public void minimize() {
            }

            @Override
            public void toggleMaximized() {
            }

            @Override
            public void requestClose() {
            }
        };
    }

    private static Container referenceControls(TitleBar titleBar) {
        return titleBar.controlButtons().get(0).getParent();
    }

    private static void layoutTitleBar(TitleBar titleBar, int width, int height) {
        titleBar.setSize(width, height);
        titleBar.doLayout();
        referenceControls(titleBar).doLayout();
    }

    private static JLabel titleLabel(TitleBar titleBar) {
        return Arrays.stream(titleBar.getComponents())
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static List<WindowControlButton.Control> visualControlOrder(Container controls) {
        return Arrays.stream(controls.getComponents())
                .map(WindowControlButton.class::cast)
                .map(WindowControlButton::control)
                .toList();
    }

    private static void invokeFocusedKey(WindowControlButton button, KeyStroke keyStroke) {
        Object actionKey = button.getInputMap().get(keyStroke);
        assertNotNull(actionKey);
        Action action = button.getActionMap().get(actionKey);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "keyboard"));
    }

    private static BufferedImage render(WindowControlButton button) {
        Dimension size = button.getPreferredSize();
        button.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Color pixel(BufferedImage image, int x, int y) {
        return new Color(image.getRGB(x, y), true);
    }

    private static WindowChrome.ResizeHandle handle(WindowChrome chrome, ResizeEdge edge) {
        return chrome.resizeHandles().stream()
                .filter(handle -> handle.edge() == edge)
                .findFirst()
                .orElseThrow();
    }

    private void setLafDefault(String key, Object value) {
        saveDefault(key);
        UIManager.getLookAndFeelDefaults().put(key, value);
    }

    private void removeLafDefault(String key) {
        saveDefault(key);
        UIManager.getLookAndFeelDefaults().remove(key);
    }

    private void saveDefault(String key) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        savedDefaults.computeIfAbsent(
                key,
                ignored -> new SavedDefault(defaults.containsKey(key), defaults.get(key))
        );
    }

    private record SavedDefault(boolean present, Object value) {
    }
}
