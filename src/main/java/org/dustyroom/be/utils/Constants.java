package org.dustyroom.be.utils;

import java.util.List;

public class Constants {

    // TODO Consider config. H2?
    public final static List<String> FILES_BLACKLIST = List.of(".DS_Store");

    public final static String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif"};
}
