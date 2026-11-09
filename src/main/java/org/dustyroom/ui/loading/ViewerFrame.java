package org.dustyroom.ui.loading;

import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.be.models.PageKey;

import java.awt.image.BufferedImage;
import java.io.File;

public record ViewerFrame(BufferedImage image, String title, File directory,
                          int firstPage, int lastPage, int pageCount,
                          boolean twoPageMode, ReadingMode readingMode, PageKey pageKey) {
}
