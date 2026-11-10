package org.dustyroom.be.models.state;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.dustyroom.be.models.direction.ReadingMode;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class ReaderSettings {
    private ViewerTheme theme;
    private FitMode fitMode;
    private double zoom;
    private String lastDirectory;
    private boolean thumbnailsVisible;
    private boolean groupRecentByFolder;

    private ThemeMode themeMode;
    private ReadingMode defaultReadingMode;
    private boolean defaultTwoPageMode;
    private FitMode defaultFitMode;
    private String libraryDirectory;

    @Builder(toBuilder = true)
    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory,
                          boolean thumbnailsVisible, boolean groupRecentByFolder, ThemeMode themeMode,
                          ReadingMode defaultReadingMode, boolean defaultTwoPageMode, FitMode defaultFitMode,
                          String libraryDirectory) {
        lastDirectory = lastDirectory == null ? "" : lastDirectory;
        if (theme == null || fitMode == null || !Double.isFinite(zoom) || zoom < 0.05 || zoom > 20) {
            throw new IllegalArgumentException("Invalid reader settings");
        }

        this.theme = theme;
        this.fitMode = fitMode;
        this.zoom = zoom;
        this.lastDirectory = lastDirectory;
        this.thumbnailsVisible = thumbnailsVisible;
        this.groupRecentByFolder = groupRecentByFolder;
        this.themeMode = themeMode == null ? (theme == ViewerTheme.LIGHT ? ThemeMode.LIGHT : ThemeMode.DARK) : themeMode;
        this.defaultReadingMode = defaultReadingMode == null ? ReadingMode.MANGA : defaultReadingMode;
        this.defaultTwoPageMode = defaultTwoPageMode;
        this.defaultFitMode = defaultFitMode == null ? FitMode.FIT_SCREEN : defaultFitMode;
        this.libraryDirectory = libraryDirectory == null ? "" : libraryDirectory;
    }

    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory,
                          boolean thumbnailsVisible, boolean groupRecentByFolder) {
        this(theme, fitMode, zoom, lastDirectory, thumbnailsVisible, groupRecentByFolder,
                null, ReadingMode.MANGA, false, fitMode, "");
    }

    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory, boolean thumbnailsVisible) {
        this(theme, fitMode, zoom, lastDirectory, thumbnailsVisible, true);
    }

    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom, String lastDirectory) {
        this(theme, fitMode, zoom, lastDirectory, true);
    }

    public ReaderSettings(ViewerTheme theme, FitMode fitMode, double zoom) {
        this(theme, fitMode, zoom, "");
    }

    public static ReaderSettings defaults() {
        return new ReaderSettings(ViewerTheme.DARK, FitMode.FIT_SCREEN, 1.0);
    }
    public ReaderSettings setTheme(ViewerTheme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Invalid reader settings");
        }
        this.theme = theme;
        return this;
    }

    public ReaderSettings setFitMode(FitMode fitMode) {
        if (fitMode == null) {
            throw new IllegalArgumentException("Invalid reader settings");
        }
        this.fitMode = fitMode;
        return this;
    }

    public ReaderSettings setZoom(double zoom) {
        if (!Double.isFinite(zoom) || zoom < 0.05 || zoom > 20) {
            throw new IllegalArgumentException("Invalid reader settings");
        }
        this.zoom = zoom;
        return this;
    }

    public ReaderSettings setLastDirectory(String lastDirectory) {
        this.lastDirectory = lastDirectory == null ? "" : lastDirectory;
        return this;
    }
}
