package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static org.dustyroom.be.utils.Constants.*;

@UtilityClass
public class FileUtils {

    public final static Predicate<File[]> isNotEmpty = f -> f != null && f.length > 0;
    public final static Predicate<File> isImage = f -> f != null && f.isFile() && isSupportedImage(f.getName());
    public final static Predicate<File> isDirectory = f -> f != null && f.isDirectory();
    public final static Predicate<File> isNotEmptyDirectory =
            f -> isDirectory.test(f) && isNotEmpty.test(f.listFiles());
    public final static Predicate<File> containsImages =
            directory -> {
                File[] children = directory == null ? null : directory.listFiles();
                return children != null && Arrays.stream(children)
                        .anyMatch(isImage);
            };
    public final static Predicate<File> isZipFile =
            f -> f != null && f.isFile() && isSupportedArchive(f.getName());

    public static boolean isSupportedImage(String path) {
        return isAllowedPath(path) && extensionOf(path).filter(IMAGE_FORMATS::contains).isPresent();
    }

    public static boolean isSupportedArchive(String path) {
        return isAllowedPath(path) && extensionOf(path).filter(ARCHIVE_FORMATS::contains).isPresent();
    }

    public static Optional<String> extensionOf(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        int separatorIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int extensionIndex = path.lastIndexOf('.');
        if (extensionIndex <= separatorIndex + 1 || extensionIndex == path.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(path.substring(extensionIndex + 1).toLowerCase(Locale.ROOT));
    }

    private static boolean isAllowedPath(String path) {
        return path != null && FILTERED_PATH_PARTS
                .stream()
                .noneMatch(path::contains);
    }

    public static List<File> getSortedFilesFromParent(File file, Predicate<File> predicate) {
        return file == null ? List.of() : getSortedFiles(file.getParentFile(), predicate);
    }

    public static List<File> getSortedFiles(File file, Predicate<File> predicate) {
        File[] children = file == null ? null : file.listFiles();
        if (children == null) {
            return List.of();
        }

        return Arrays.stream(children)
                .filter(predicate)
                .sorted(Comparator.comparing(File::getName, NaturalOrderComparator.INSTANCE))
                .toList();
    }

    public static File normalize(File file) {
        return file == null ? null : file.toPath().toAbsolutePath().normalize().toFile();
    }

    public static String normalizedPath(File file) {
        return file == null ? "" : normalize(file).getPath();
    }

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
