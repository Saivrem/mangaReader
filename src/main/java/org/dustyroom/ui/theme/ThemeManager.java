package org.dustyroom.ui.theme;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.LookSettings;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.dustyroom.ui.LookSettings.MANGA_READER_DARK;
import static org.dustyroom.ui.LookSettings.MANGA_READER_LIGHT;

@Slf4j
public final class ThemeManager {
    public static final LookSettings DEFAULT_THEME = MANGA_READER_DARK;
    static final LookSettings FALLBACK_THEME = MANGA_READER_LIGHT;

    private static volatile LookSettings current;

    private ThemeManager() {
    }

    /**
     * Installs the application theme before any Swing component is created.
     * The branded light theme is used as a fallback if the default dark theme
     * cannot be installed.
     */
    public static boolean installDefault() {
        if (install(DEFAULT_THEME)) {
            return true;
        }
        log.warn("Falling back to {} look and feel", FALLBACK_THEME.getDisplayName());
        return install(FALLBACK_THEME);
    }

    /**
     * Installs a look and feel without touching existing component trees.
     * Prefer this method during application startup.
     */
    public static synchronized boolean install(LookSettings theme) {
        Objects.requireNonNull(theme, "theme");
        try {
            UIManager.setLookAndFeel(Objects.requireNonNull(
                    theme.createLookAndFeel(),
                    () -> "No look and feel factory for " + theme
            ));
            current = theme;
            return true;
        } catch (javax.swing.UnsupportedLookAndFeelException | RuntimeException e) {
            log.error("Can't install {} look and feel", theme.getDisplayName(), e);
            return false;
        }
    }

    /**
     * Applies a theme at runtime and refreshes the supplied windows on the EDT.
     */
    public static boolean apply(LookSettings theme, Window... windows) {
        return applyToComponentTrees(theme, windows);
    }

    /**
     * Applies a theme to every application window, including currently open
     * dialogs and heavyweight popup windows.
     */
    public static boolean applyToOpenWindows(LookSettings theme) {
        return apply(theme, Window.getWindows());
    }

    /**
     * Component-root variant used by headless tests and embedders that do not
     * own a top-level window.
     */
    public static boolean applyToComponentTrees(LookSettings theme, Component... roots) {
        Objects.requireNonNull(theme, "theme");
        Component[] targets = roots == null ? new Component[0] : roots.clone();
        if (SwingUtilities.isEventDispatchThread()) {
            return applyOnEdt(theme, targets);
        }

        AtomicBoolean applied = new AtomicBoolean();
        try {
            SwingUtilities.invokeAndWait(() -> applied.set(applyOnEdt(theme, targets)));
            return applied.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while applying {} look and feel", theme.getDisplayName(), e);
            return false;
        } catch (InvocationTargetException e) {
            log.error("Can't refresh components after applying {} look and feel", theme.getDisplayName(), e.getCause());
            return false;
        }
    }

    public static LookSettings current() {
        return current;
    }

    private static boolean applyOnEdt(LookSettings theme, Component[] roots) {
        if (!install(theme)) {
            return false;
        }
        for (Component root : roots) {
            if (root == null) {
                continue;
            }
            SwingUtilities.updateComponentTreeUI(root);
            root.invalidate();
            root.validate();
            root.repaint();
        }
        return true;
    }
}
