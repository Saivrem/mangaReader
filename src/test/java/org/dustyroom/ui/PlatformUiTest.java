package org.dustyroom.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUiTest {

    @Test
    void detectsMacOsNames() {
        assertTrue(PlatformUi.isMacOs("Mac OS X"));
        assertTrue(PlatformUi.isMacOs("Darwin"));
    }

    @Test
    void doesNotTreatWindowsOrLinuxAsMacOs() {
        assertFalse(PlatformUi.isMacOs("Windows 11"));
        assertFalse(PlatformUi.isMacOs("Linux"));
        assertFalse(PlatformUi.isMacOs(""));
    }
}
