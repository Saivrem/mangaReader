package org.dustyroom.ui;

import org.dustyroom.ui.theme.MangaReaderDarkLaf;
import org.dustyroom.ui.theme.MangaReaderLightLaf;

import javax.swing.LookAndFeel;
import java.util.function.Supplier;

public enum LookSettings {

    MANGA_READER_DARK("Manga Reader Dark", MangaReaderDarkLaf::new),
    MANGA_READER_LIGHT("Manga Reader Light", MangaReaderLightLaf::new);

    private final String displayName;
    private final Supplier<LookAndFeel> lookAndFeelFactory;

    LookSettings(String displayName, Supplier<LookAndFeel> lookAndFeelFactory) {
        this.displayName = displayName;
        this.lookAndFeelFactory = lookAndFeelFactory;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Creates a fresh look-and-feel instance so repeated runtime theme changes
     * do not reuse mutable Swing defaults from an earlier installation.
     */
    public LookAndFeel createLookAndFeel() {
        return lookAndFeelFactory.get();
    }

    /**
     * Kept for source compatibility with the previous enum API.
     */
    public LookAndFeel getLookAndFeel() {
        return createLookAndFeel();
    }
}
