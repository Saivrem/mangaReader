package org.dustyroom.persistence;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePathsTest {
    @Test
    void resolvesPlatformDirectoriesAndPortableOverrideWithoutAccessingThem() {
        // Use host-native absolute paths so the same cases also run on Windows.
        Path root = Path.of("build", "path-fixtures").toAbsolutePath();
        String home = root.resolve("home").toString();
        String appData = root.resolve("appdata").toString();
        String xdg = root.resolve("xdg").toString();
        String portable = root.resolve("portable").toString();
        assertEquals(Path.of(home, "Library", "Application Support", "MangaReader"),
                StoragePaths.directory(null, "Mac OS X", home, Map.of()));
        assertEquals(Path.of(appData, "MangaReader"),
                StoragePaths.directory(null, "Windows 11", home, Map.of("APPDATA", appData)));
        assertEquals(Path.of(xdg, "manga-reader"),
                StoragePaths.directory(null, "Linux", home, Map.of("XDG_DATA_HOME", xdg)));
        assertEquals(Path.of(home, ".local", "share", "manga-reader"),
                StoragePaths.directory(null, "Linux", home, Map.of("XDG_DATA_HOME", "relative")));
        assertEquals(Path.of(portable), StoragePaths.directory(portable, "Mac OS X", home, Map.of()));
    }
}
