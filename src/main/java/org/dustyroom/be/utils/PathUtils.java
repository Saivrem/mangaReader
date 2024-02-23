package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Arrays;

import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;

@UtilityClass
public class PathUtils {

    public static String getFileName(Path path, int offset) {
        return path.getName(path.getNameCount() - offset).toString();
    }

    public static boolean isNotImage(Path image) {
        if (image == null) return true;
        String fileName = getFileName(image, 1);
        return Arrays.stream(SUPPORTED_FORMATS).noneMatch(fileName::endsWith);
    }
}
