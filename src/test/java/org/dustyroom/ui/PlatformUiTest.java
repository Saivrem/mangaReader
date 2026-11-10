package org.dustyroom.ui;

import org.dustyroom.be.utils.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUiTest {
    @Test
    void finderOpenRequestsUseTheEdtAndPreservePaths() throws Exception {
        List<File> opened = new ArrayList<File>();
        File first = new File("日本語 books/volume 01.zip");
        PlatformUi.openFiles(List.of(first, new File("second.zip")), file -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            opened.add(file);
        });
        PlatformUi.openFiles(List.of(), file -> {
            throw new AssertionError("Empty event");
        });
        SwingUtilities.invokeAndWait(() -> {
        });
        Assertions.assertEquals(List.of(first), opened);
    }

    @Test
    void macAssociationsMatchSupportedFormats() throws Exception {
        Set<String> extensions = new HashSet<String>();
        try (Stream<Path> paths = Files.list(Path.of("packaging/macos/associations"))) {
            for (Path path : paths.toList()) {
                Properties properties = new Properties();
                try (InputStream stream = Files.newInputStream(path)) {
                    properties.load(stream);
                }
                extensions.add(properties.getProperty("extension"));
            }
        }
        Assertions.assertEquals(
                Set.of(Constants.SUPPORTED_FORMATS), extensions);
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
