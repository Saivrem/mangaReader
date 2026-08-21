package org.dustyroom.ui.window.platform;

import java.util.Locale;

/** Operating-system family used to select application-drawn window chrome. */
public enum DesktopPlatform {
    MACOS,
    WINDOWS,
    LINUX,
    OTHER;

    public static DesktopPlatform current() {
        return detect(System.getProperty("os.name", ""));
    }

    public static DesktopPlatform detect(String osName) {
        String normalized = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return MACOS;
        }
        if (normalized.contains("win")) {
            return WINDOWS;
        }
        if (normalized.contains("linux")) {
            return LINUX;
        }
        return OTHER;
    }
}
