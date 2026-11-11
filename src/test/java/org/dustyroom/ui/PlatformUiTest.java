package org.dustyroom.ui;

import org.dustyroom.be.utils.Constants;
import org.dustyroom.ui.utils.PlatformUi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUiTest {
    @Test
    void finderOpenRequestsUseTheEdtAndPreservePaths() throws Exception {
        List<File> opened = new ArrayList<>();
        File first = new File("日本語 books/volume 01.zip");
        PlatformUi.openFiles(List.of(first, new File("second.zip")), file -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            opened.add(file);
        });
        PlatformUi.openFiles(List.of(), _ -> {
            throw new AssertionError("Empty event");
        });
        SwingUtilities.invokeAndWait(() -> {
        });
        Assertions.assertEquals(List.of(first), opened);
    }

    @Test
    void macAssociationsMatchSupportedFormats() throws Exception {
        Set<String> extensions = new HashSet<>();
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
}
