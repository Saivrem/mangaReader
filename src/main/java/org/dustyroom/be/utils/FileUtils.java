package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
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
            f -> isDirectory.test(f) && !getSortedFiles(f, isImage).isEmpty();
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
        return path != null && FILTERED_PATH_PARTS.stream().noneMatch(path::contains);
    }

    public static List<File> getSortedFilesFromParent(File file, Predicate<File> predicate) {
        return file == null ? List.of() : getSortedFiles(file.getParentFile(), predicate);
    }

    public static List<File> getSortedFiles(File file, Predicate<File> predicate) {
        return Optional.ofNullable(file).stream()
                .filter(File::isDirectory)
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .filter(predicate)
                .sorted((o1, o2) -> NaturalOrderComparator.INSTANCE.compare(o1.getName(), o2.getName()))
                .toList();
    }
}
