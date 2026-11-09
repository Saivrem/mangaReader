package org.dustyroom.persistence;

import org.dustyroom.ui.components.ImageComponent.FitMode;
import org.dustyroom.ui.theme.ViewerTheme;

public record ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory, boolean thumbnailsVisible) {
    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory) {
        this(theme, fitMode, zoom, lastDirectory, true);
    }

    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom) {
        this(theme, fitMode, zoom, "");
    }

    public static ReaderSettings defaults() { return new ReaderSettings(ViewerTheme.DARK, FitMode.FIT_SCREEN, 1.0); }

    public ReaderSettings {
        lastDirectory = lastDirectory == null ? "" : lastDirectory;
        if (theme == null || fitMode == null || !Double.isFinite(zoom) || zoom < 0.05 || zoom > 20) {
            throw new IllegalArgumentException("Invalid reader settings");
        }
    }
}
