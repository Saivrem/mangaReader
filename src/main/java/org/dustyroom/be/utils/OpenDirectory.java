package org.dustyroom.be.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

/**
 * Filesystem checks belong on a worker thread, before constructing the chooser.
 */
@UtilityClass
public class OpenDirectory {

    public static File resolve(String saved, String home) {
        if (saved != null && !saved.isBlank()) {
            try {
                Path path = Path.of(saved);
                if (path.isAbsolute() && Files.isDirectory(path) && Files.isReadable(path)) {
                    return path.toFile();
                }
            } catch (InvalidPathException | SecurityException ignored) { /* fall back to home */ }
        }
        return new File(home);
    }
}
