package org.dustyroom.be.utils;

import java.util.List;

public class Constants {
    // TODO Consider config. H2?
    public final static String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif", "zip"};
    public final static List<String> TO_FILTER = List.of("__MACOS");
    public final static String ABOUT = """
            Manga Reader
                        
            Version 0.4.3
            Author: Denys Sheviakov
            """;
}
