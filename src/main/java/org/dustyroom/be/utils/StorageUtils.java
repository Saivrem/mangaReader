package org.dustyroom.be.utils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.settings.ProfileManager;

@UtilityClass
public final class StorageUtils {

    public static Path database() {
        return ProfileManager.resolve(standardDirectory(), System.getProperty("yonaka.dataDir"));
    }

    public static Path standardDirectory() {
        return directory(null, System.getProperty("os.name", ""), System.getProperty("user.home"), System.getenv());
    }

    public static boolean isDirectoryOverridden() {
        String override = System.getProperty("yonaka.dataDir");
        return override != null && !override.isBlank();
    }

    private static boolean isMac(String os) {
        String platform = os.toLowerCase(Locale.ROOT);
        return platform.startsWith("mac") || platform.startsWith("darwin");
    }

    private static boolean isWindows(String os) {
        return os.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    static Path directory(String override, String os, String home, Map<String, String> environment) {
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        if (isMac(os)) {
            return Path.of(home, "Library", "Application Support", "YonakaReader");
        }
        if (isWindows(os)) {
            String appData = environment.get("APPDATA");
            return (appData == null || appData.isBlank() ? Path.of(home, "AppData", "Roaming") : Path.of(appData))
                    .resolve("YonakaReader");
        }
        String xdg = environment.get("XDG_DATA_HOME");
        Path root = xdg != null && !xdg.isBlank() && Path.of(xdg).isAbsolute()
                ? Path.of(xdg) : Path.of(home, ".local", "share");
        return root.resolve("yonaka-reader");
    }
}
