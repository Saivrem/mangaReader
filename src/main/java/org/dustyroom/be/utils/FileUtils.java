package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.Constants.TO_FILTER;

@UtilityClass
public class FileUtils {

    public final static Predicate<File[]> isNotEmpty = f -> f != null && f.length > 0;
    public final static Predicate<File> isImage = f -> f != null && f.isFile() && isSupported(f.getName());
    public final static Predicate<File> isDirectory = f -> f != null && f.isDirectory();
    public final static Predicate<File> isNotEmptyDirectory = f -> isDirectory.test(f) && isNotEmpty.test(f.listFiles());
    public final static Predicate<File> isZipFile = f -> f != null && f.isFile() && f.getName().endsWith(".zip");


    public static boolean isSupported(String string) {
        boolean notTrashFile = TO_FILTER.stream().noneMatch(string::contains);
        boolean supportedFormat = Arrays.stream(SUPPORTED_FORMATS).anyMatch(string::endsWith);
        return notTrashFile && supportedFormat;
    }

    public static List<File> getSortedFilesFromParent(File file, Predicate<File> predicate) {
        return getSortedFiles(file.getParentFile(), predicate);
    }

    public static List<File> getSortedFiles(File file, Predicate<File> predicate) {
        return Optional.ofNullable(file).stream()
                .filter(File::isDirectory)
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .filter(predicate)
                .sorted((o1, o2) -> {
                    String o1Name = o1.getName();
                    String o2Name = o2.getName();

                    return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                })
                .toList();
    }
}
