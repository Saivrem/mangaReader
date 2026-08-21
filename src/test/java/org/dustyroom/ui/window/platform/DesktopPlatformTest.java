package org.dustyroom.ui.window.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DesktopPlatformTest {
    @Test
    void detectsSupportedDesktopFamiliesWithoutLocaleSensitiveMatching() {
        assertAll(
                () -> assertEquals(DesktopPlatform.MACOS, DesktopPlatform.detect("Mac OS X")),
                () -> assertEquals(DesktopPlatform.MACOS, DesktopPlatform.detect("Darwin")),
                () -> assertEquals(DesktopPlatform.WINDOWS, DesktopPlatform.detect("Windows 11")),
                () -> assertEquals(DesktopPlatform.LINUX, DesktopPlatform.detect("Linux")),
                () -> assertEquals(DesktopPlatform.OTHER, DesktopPlatform.detect("FreeBSD")),
                () -> assertEquals(DesktopPlatform.OTHER, DesktopPlatform.detect(null))
        );
    }
}
