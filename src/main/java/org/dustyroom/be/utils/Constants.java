package org.dustyroom.be.utils;

public class Constants {
    // TODO Consider config. H2?
    public final static String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "gif"};
    public final static String DARK_THEME = "dark";
    public final static String METAL_THEME = "metal";
    public final static String ABOUT = """
            Manga Reader
                        
            Hotkeys:
            Open file   : O
            Next image  : -> / Pg Down
            Prev image  : <- / Pg Up
            First image : Home
            Last image  : End
            Quit        : Esc / Q
                        
            Version 0.1
            Author: Denys Sheviakov
            """;
}
