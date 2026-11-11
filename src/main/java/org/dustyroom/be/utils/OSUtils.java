package org.dustyroom.be.utils;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class OSUtils {
    public static String currentName() {
        return System.getProperty("os.name", "");
    }

    public static boolean isMac(String os) {
        String platform = os.toLowerCase(Locale.ROOT);
        return platform.startsWith("mac") || platform.startsWith("darwin");
    }

    public static boolean isWindows(String os) {
        return os.toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
