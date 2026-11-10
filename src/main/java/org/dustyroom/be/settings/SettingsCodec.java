package org.dustyroom.be.settings;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.models.state.ViewerTheme;

@UtilityClass
public class SettingsCodec {
    public static ReaderSettings decode(Map<String, String> values) {
        ReaderSettings defaults = ReaderSettings.defaults();
        ViewerTheme theme = choice(ViewerTheme.class, values.get("theme"), defaults.getTheme());
        FitMode fit = choice(FitMode.class, values.get("fit_mode"), defaults.getFitMode());
        double zoom = defaults.getZoom();
        try {
            double parsed = Double.parseDouble(values.getOrDefault("zoom", "1"));
            if (Double.isFinite(parsed) && parsed >= 0.05 && parsed <= 20) {
                zoom = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Preserve other valid settings.
        }
        ThemeMode previousTheme = theme == ViewerTheme.LIGHT ? ThemeMode.LIGHT : ThemeMode.DARK;
        return new ReaderSettings(theme, fit, zoom, values.getOrDefault("last_directory", ""),
                !"false".equals(values.get("thumbnails_visible")), !"false".equals(values.get("group_recent_by_folder")),
                choice(ThemeMode.class, values.get("theme_mode"), previousTheme),
                choice(ReadingMode.class, values.get("default_reading_mode"), ReadingMode.MANGA),
                "true".equals(values.get("default_two_page")),
                choice(FitMode.class, values.get("default_fit_mode"), fit),
                values.getOrDefault("library_directory", ""));
    }

    public static Map<String, String> encode(ReaderSettings settings) {
        return Map.ofEntries(
                Map.entry("theme", settings.getTheme().name()),
                Map.entry("fit_mode", settings.getFitMode().name()),
                Map.entry("zoom", Double.toString(settings.getZoom())),
                Map.entry("last_directory", settings.getLastDirectory()),
                Map.entry("thumbnails_visible", Boolean.toString(settings.isThumbnailsVisible())),
                Map.entry("group_recent_by_folder", Boolean.toString(settings.isGroupRecentByFolder())),
                Map.entry("theme_mode", settings.getThemeMode().name()),
                Map.entry("default_reading_mode", settings.getDefaultReadingMode().name()),
                Map.entry("default_two_page", Boolean.toString(settings.isDefaultTwoPageMode())),
                Map.entry("default_fit_mode", settings.getDefaultFitMode().name()),
                Map.entry("library_directory", settings.getLibraryDirectory()));
    }

    private static <E extends Enum<E>> E choice(Class<E> type, String value, E fallback) {
        try {
            return value == null ? fallback : Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
