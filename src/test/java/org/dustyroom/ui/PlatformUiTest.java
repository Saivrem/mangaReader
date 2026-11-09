package org.dustyroom.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUiTest {
    @Test
    void finderOpenRequestsUseTheEdtAndPreservePaths() throws Exception {
        var opened = new java.util.ArrayList<java.io.File>();
        var first = new java.io.File("日本語 books/volume 01.zip");
        PlatformUi.openFiles(java.util.List.of(first, new java.io.File("second.zip")), file -> {
            assertTrue(javax.swing.SwingUtilities.isEventDispatchThread());
            opened.add(file);
        });
        PlatformUi.openFiles(java.util.List.of(), file -> { throw new AssertionError("Empty event"); });
        javax.swing.SwingUtilities.invokeAndWait(() -> {});
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(first), opened);
    }

    @Test
    void macAssociationsMatchSupportedFormats() throws Exception {
        var extensions = new java.util.HashSet<String>();
        try (var paths = java.nio.file.Files.list(java.nio.file.Path.of("packaging/macos/associations"))) {
            for (var path : paths.toList()) {
                var properties = new java.util.Properties();
                try (var stream = java.nio.file.Files.newInputStream(path)) { properties.load(stream); }
                extensions.add(properties.getProperty("extension"));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(
                java.util.Set.of(org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS), extensions);
    }


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
