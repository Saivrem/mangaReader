package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.Constants.TO_FILTER;

@UtilityClass
public class FileUtils {

    public static boolean isSupported(String string) {
        boolean notTrashFile = TO_FILTER.stream().noneMatch(string::contains);
        boolean supportedFormat = Arrays.stream(SUPPORTED_FORMATS).anyMatch(string::endsWith);
        return notTrashFile && supportedFormat;
    }
}
