package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.dustyroom.be.settings.ProfileManager;

import java.nio.file.Path;
import java.util.Map;

import static org.dustyroom.be.utils.OSUtils.isMac;
import static org.dustyroom.be.utils.OSUtils.isWindows;

@UtilityClass
public final class StorageUtils {

    private static final String DATA_DIRECTORY = "yonaka.dataDir";

    public static Path database() {
        return ProfileManager.resolve(standardDirectory(), System.getProperty(DATA_DIRECTORY));
    }

    public static Path standardDirectory() {
        return directory(OSUtils.currentName(), System.getProperty("user.home"), System.getenv());
    }

    public static boolean isDirectoryOverridden() {
        String override = System.getProperty(DATA_DIRECTORY);
        return StringUtils.isNotBlank(override);
    }

    static Path directory(String os, String home, Map<String, String> environment) {
        if (isMac(os)) {
            return Path.of(home, "Library", "Application Support", "YonakaReader");
        }

        if (isWindows(os)) {
            String appData = environment.get("APPDATA");
            Path root = StringUtils.isNotBlank(appData) ? Path.of(appData) : Path.of(home, "AppData", "Roaming");
            return root.resolve("YonakaReader");
        }

        Path fallback = Path.of(home, ".local", "share");
        String xdg = environment.get("XDG_DATA_HOME");
        Path root = StringUtils.isNotBlank(xdg) ? Path.of(xdg) : fallback;
        if (!root.isAbsolute()) {
            root = fallback;
        }

        return root.resolve("yonaka-reader");
    }
}
