package org.dustyroom.be.utils;

import java.util.List;

public class Constants {
    // TODO Consider config. H2?
    public final static List<String> FILES_BLACKLIST = List.of(".DS_Store");
    public final static String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif"};
    public final static String DARK_THEME = "dark";
    public final static String ABOUT = "Manga Reader\nVersion 0.1\nAuthor: Denys Sheviakov";
}
