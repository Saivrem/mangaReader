package org.dustyroom.be.utils;

import java.util.List;
import java.util.stream.Stream;

public final class Constants {
    public static final List<String> IMAGE_FORMATS = List.of("jpg", "jpeg", "png", "gif");
    public static final List<String> ARCHIVE_FORMATS = List.of("zip");
    public static final String[] SUPPORTED_FORMATS = Stream.concat(
            IMAGE_FORMATS.stream(),
            ARCHIVE_FORMATS.stream()
    ).toArray(String[]::new);
    public static final List<String> FILTERED_PATH_PARTS = List.of("__MACOSX", ".DS_Store");
    public static final String ABOUT = """
            Manga Reader
            
            Version %s
            Author: Denys Sheviakov
            """;

    private Constants() {
    }
}
