package org.dustyroom.be.settings;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.persistence.SqliteReaderStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SettingsCodecTest {
    @TempDir
    Path root;

    @Test
    void oldPreferencesKeepTheirThemeAndScale() {
        ReaderSettings settings = SettingsCodec.decode(Map.of("theme", "LIGHT", "fit_mode", "FIT_WIDTH"));
        assertEquals(ThemeMode.LIGHT, settings.getThemeMode());
        assertEquals(FitMode.FIT_WIDTH, settings.getDefaultFitMode());
        assertEquals(ReadingMode.MANGA, settings.getDefaultReadingMode());
        assertFalse(settings.isDefaultTwoPageMode());
    }

    @Test
    void persistsNewPreferencesWithoutSchemaUpgrade() {
        ReaderSettings expected = ReaderSettings.defaults().toBuilder().themeMode(ThemeMode.SYSTEM)
                .defaultReadingMode(ReadingMode.COMICS).defaultTwoPageMode(true)
                .defaultFitMode(FitMode.FIT_HEIGHT).libraryDirectory(root.toString()).build();
        Path database = root.resolve("reader.sqlite3");
        try (SqliteReaderStore store = new SqliteReaderStore(database)) {
            store.saveSettings(expected);
        }
        try (SqliteReaderStore store = new SqliteReaderStore(database)) {
            assertEquals(expected, store.settings());
        }
    }

    @Test
    void parsesWindowsAppearanceWithoutGuessingOnFailure() {
        assertEquals(Optional.of(ViewerTheme.DARK), SystemThemeDetector.parseWindows("    AppsUseLightTheme    REG_DWORD    0x0\r\n"));
        assertEquals(Optional.of(ViewerTheme.LIGHT), SystemThemeDetector.parseWindows("    AppsUseLightTheme    REG_DWORD    0x1\r\n"));
        assertEquals(Optional.empty(), SystemThemeDetector.parseWindows("ERROR: The system was unable to find the registry key."));
    }
}
