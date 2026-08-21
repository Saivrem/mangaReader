package org.dustyroom.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Cross-platform default look and feel for Manga Reader.
 *
 * <p>The matching {@code MangaReaderDarkLaf.properties} resource owns the
 * palette and component defaults so window chrome and regular Swing controls
 * can consume the same design tokens.</p>
 */
public final class MangaReaderDarkLaf extends FlatDarkLaf {
    public static boolean setup() {
        return setup(new MangaReaderDarkLaf());
    }

    @Override
    public String getName() {
        return "Manga Reader Dark";
    }

    @Override
    public String getDescription() {
        return "Manga Reader cross-platform dark look and feel";
    }
}
