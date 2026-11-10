package org.dustyroom.be.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageUtilsTest {
    @Test
    void resolvesPlatformDirectoriesAndPortableOverrideWithoutAccessingThem() {
        // Use host-native absolute paths so the same cases also run on Windows.
        Path root = Path.of("build", "path-fixtures").toAbsolutePath();
        String home = root.resolve("home").toString();
        String appData = root.resolve("appdata").toString();
        String xdg = root.resolve("xdg").toString();
        String portable = root.resolve("portable").toString();
        assertEquals(Path.of(home, "Library", "Application Support", "YonakaReader"),
                StorageUtils.directory(null, "Mac OS X", home, Map.of()));
        assertEquals(Path.of(appData, "YonakaReader"),
                StorageUtils.directory(null, "Windows 11", home, Map.of("APPDATA", appData)));
        assertEquals(Path.of(xdg, "yonaka-reader"),
                StorageUtils.directory(null, "Linux", home, Map.of("XDG_DATA_HOME", xdg)));
        assertEquals(Path.of(home, ".local", "share", "yonaka-reader"),
                StorageUtils.directory(null, "Linux", home, Map.of("XDG_DATA_HOME", "relative")));
        assertEquals(Path.of(portable), StorageUtils.directory(portable, "Mac OS X", home, Map.of()));
    }
}
