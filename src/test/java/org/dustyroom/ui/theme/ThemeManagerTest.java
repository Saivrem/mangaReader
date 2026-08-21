package org.dustyroom.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.dustyroom.ui.LookSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("Swing UIManager defaults")
class ThemeManagerTest {
    private static final Set<String> APPLICATION_TOKEN_KEYS = Set.of(
            "MangaReader.canvasBackground",
            "MangaReader.titleBarBackground",
            "MangaReader.titleBarForeground",
            "MangaReader.titleBarInactiveBackground",
            "MangaReader.titleBarInactiveForeground",
            "MangaReader.titleBarBorder",
            "MangaReader.titleBarHeight",
            "MangaReader.titleBarButtonSize",
            "MangaReader.titleBarButtonHoverBackground",
            "MangaReader.titleBarButtonPressedBackground",
            "MangaReader.titleBarCloseHoverBackground",
            "MangaReader.titleBarClosePressedBackground",
            "MangaReader.macTitleBarButtonSize",
            "MangaReader.macTitleBarControlsInset",
            "MangaReader.macTrafficLightClose",
            "MangaReader.macTrafficLightMinimize",
            "MangaReader.macTrafficLightZoom",
            "MangaReader.macTrafficLightInactive",
            "MangaReader.macTrafficLightGlyph",
            "MangaReader.imageBorder",
            "MangaReader.windowResizeHandleThickness"
    );

    @AfterEach
    void restoreDefaultTheme() {
        assertTrue(ThemeManager.installDefault());
    }

    @Test
    void installsDarkThemeByDefaultAndLoadsApplicationDefaults() {
        assertTrue(ThemeManager.installDefault());

        assertEquals(LookSettings.MANGA_READER_DARK, ThemeManager.current());
        assertInstanceOf(MangaReaderDarkLaf.class, UIManager.getLookAndFeel());
        assertInstanceOf(FlatDarkLaf.class, UIManager.getLookAndFeel());
        assertEquals(new Color(0x1B1E26), UIManager.getColor("Panel.background"));
        assertEquals(new Color(0x111318), UIManager.getColor("MangaReader.canvasBackground"));
        assertEquals(new Color(0x20242D), UIManager.getColor("MangaReader.titleBarBackground"));
        assertEquals(30, UIManager.getInt("MangaReader.titleBarHeight"));
        assertEquals(new Dimension(44, 30), UIManager.getDimension("MangaReader.titleBarButtonSize"));
        assertEquals(5, UIManager.getInt("MangaReader.windowResizeHandleThickness"));
        assertEquals(10, UIManager.getInt("Component.arc"));
        assertTrue(UIManager.getBoolean("ScrollPane.smoothScrolling"));
    }

    @Test
    void installsLightThemeAndLoadsMatchingApplicationDefaults() {
        assertTrue(ThemeManager.install(LookSettings.MANGA_READER_LIGHT));

        assertEquals(LookSettings.MANGA_READER_LIGHT, ThemeManager.current());
        assertInstanceOf(MangaReaderLightLaf.class, UIManager.getLookAndFeel());
        assertInstanceOf(FlatLightLaf.class, UIManager.getLookAndFeel());
        assertEquals(new Color(0xF5F7FB), UIManager.getColor("Panel.background"));
        assertEquals(new Color(0x20242D), UIManager.getColor("Panel.foreground"));
        assertEquals(new Color(0xFFFFFF), UIManager.getColor("Button.background"));
        assertEquals(new Color(0x20242D), UIManager.getColor("Button.foreground"));
        assertEquals(new Color(0xFFFFFF), UIManager.getColor("MenuBar.background"));
        assertEquals(new Color(0x20242D), UIManager.getColor("MenuBar.foreground"));
        assertEquals(new Color(0xECEFF4), UIManager.getColor("MangaReader.canvasBackground"));
        assertEquals(new Color(0xFFFFFF), UIManager.getColor("MangaReader.titleBarBackground"));
        assertEquals(new Color(0x20242D), UIManager.getColor("MangaReader.titleBarForeground"));
        assertEquals(new Color(0xF1F3F6), UIManager.getColor("MangaReader.titleBarInactiveBackground"));
        assertEquals(new Color(0x626A78), UIManager.getColor("MangaReader.titleBarInactiveForeground"));
        assertEquals(new Color(0x7C8798), UIManager.getColor("MangaReader.imageBorder"));
        assertEquals(new Dimension(20, 30), UIManager.getDimension("MangaReader.macTitleBarButtonSize"));
        assertNotNull(UIManager.getColor("MangaReader.macTrafficLightClose"));
        assertNotNull(UIManager.getColor("MangaReader.titleBarCloseHoverBackground"));
    }

    @Test
    void appliesRuntimeThemeWithoutRequiringAWindow() {
        assertTrue(ThemeManager.apply(LookSettings.MANGA_READER_LIGHT));

        assertEquals(LookSettings.MANGA_READER_LIGHT, ThemeManager.current());
        assertInstanceOf(MangaReaderLightLaf.class, UIManager.getLookAndFeel());
    }

    @Test
    void runtimeApplyRefreshesAnExistingHeadlessComponentTree() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(ThemeManager.installDefault());
            JPanel root = new JPanel();
            JButton button = new JButton("Theme probe");
            root.add(button);
            Color initialBackground = root.getBackground();

            assertTrue(ThemeManager.applyToComponentTrees(LookSettings.MANGA_READER_LIGHT, root));

            assertEquals(LookSettings.MANGA_READER_LIGHT, ThemeManager.current());
            assertInstanceOf(MangaReaderLightLaf.class, UIManager.getLookAndFeel());
            assertNotEquals(initialBackground, root.getBackground());
            assertEquals(UIManager.getColor("Panel.background"), root.getBackground());
            assertEquals(new Color(0xECEFF4), UIManager.getColor("MangaReader.canvasBackground"));
            assertEquals(UIManager.getColor("Button.background"), button.getBackground());
        });
    }

    @Test
    void createsFreshLookAndFeelInstances() {
        assertNotSame(
                LookSettings.MANGA_READER_DARK.createLookAndFeel(),
                LookSettings.MANGA_READER_DARK.createLookAndFeel()
        );
        assertNotSame(
                LookSettings.MANGA_READER_LIGHT.createLookAndFeel(),
                LookSettings.MANGA_READER_LIGHT.createLookAndFeel()
        );
    }

    @Test
    void exposesOnlyBrandedDarkAndLightThemes() {
        assertArrayEquals(
                new LookSettings[]{LookSettings.MANGA_READER_DARK, LookSettings.MANGA_READER_LIGHT},
                LookSettings.values()
        );
        assertEquals(LookSettings.MANGA_READER_DARK, ThemeManager.DEFAULT_THEME);
        assertEquals(LookSettings.MANGA_READER_LIGHT, ThemeManager.FALLBACK_THEME);
    }

    @Test
    void darkAndLightThemesExposeTheSameCompleteApplicationTokenContract() {
        Map<String, Object> darkTokens = installAndCaptureApplicationTokens(LookSettings.MANGA_READER_DARK);
        Map<String, Object> lightTokens = installAndCaptureApplicationTokens(LookSettings.MANGA_READER_LIGHT);

        assertEquals(APPLICATION_TOKEN_KEYS, darkTokens.keySet());
        assertEquals(darkTokens.keySet(), lightTokens.keySet());
        assertTrue(darkTokens.values().stream().allMatch(value -> value != null));
        assertTrue(lightTokens.values().stream().allMatch(value -> value != null));
    }

    private static Map<String, Object> installAndCaptureApplicationTokens(LookSettings theme) {
        assertTrue(ThemeManager.install(theme));
        Map<String, Object> tokens = new TreeMap<>();
        UIManager.getLookAndFeelDefaults().keySet().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(key -> key.startsWith("MangaReader."))
                .forEach(key -> tokens.put(key, UIManager.get(key)));
        return tokens;
    }
}
