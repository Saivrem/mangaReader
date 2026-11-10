package org.dustyroom.persistence;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class StoragePaths {
    private StoragePaths() {
    }

    public static Path database() {
        return directory(System.getProperty("mangareader.dataDir"), System.getProperty("os.name", ""),
                System.getProperty("user.home"), System.getenv()).resolve("reader.sqlite3");
    }

    static Path directory(String override, String os, String home, Map<String, String> environment) {
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize();
        String platform = os.toLowerCase(Locale.ROOT);
        if (platform.startsWith("mac") || platform.startsWith("darwin")) {
            return Path.of(home, "Library", "Application Support", "MangaReader");
        }
        if (platform.startsWith("windows")) {
            String appData = environment.get("APPDATA");
            return (appData == null || appData.isBlank() ? Path.of(home, "AppData", "Roaming") : Path.of(appData))
                    .resolve("MangaReader");
        }
        String xdg = environment.get("XDG_DATA_HOME");
        Path root = xdg != null && !xdg.isBlank() && Path.of(xdg).isAbsolute()
                ? Path.of(xdg) : Path.of(home, ".local", "share");
        return root.resolve("manga-reader");
    }
}
