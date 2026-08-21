package org.dustyroom.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Cross-platform light look and feel for Manga Reader.
 *
 * <p>The matching {@code MangaReaderLightLaf.properties} resource owns the
 * palette and the same application design tokens as the dark theme.</p>
 */
public final class MangaReaderLightLaf extends FlatLightLaf {
    public static boolean setup() {
        return setup(new MangaReaderLightLaf());
    }

    @Override
    public String getName() {
        return "Manga Reader Light";
    }

    @Override
    public String getDescription() {
        return "Manga Reader cross-platform light look and feel";
    }
}
