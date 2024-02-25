package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;

@UtilityClass
public class PathUtils {

    public static boolean isImage(Path image) {
        return Optional.ofNullable(image)
                .map(Path::toString)
                .filter(i -> Arrays.stream(SUPPORTED_FORMATS).anyMatch(i::endsWith))
                .isPresent();
    }
}
