package org.dustyroom.be.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OSUtilsTest {
    @Test
    void detectsMacOsNames() {
        assertTrue(OSUtils.isMac("Mac OS X"));
        assertTrue(OSUtils.isMac("Darwin"));
    }

    @Test
    void doesNotTreatWindowsOrLinuxAsMacOs() {
        assertFalse(OSUtils.isMac("Windows 11"));
        assertFalse(OSUtils.isMac("Linux"));
        assertFalse(OSUtils.isMac(""));
    }
}