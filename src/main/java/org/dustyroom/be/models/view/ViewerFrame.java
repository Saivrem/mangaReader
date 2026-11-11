package org.dustyroom.be.models.view;

import lombok.*;
import lombok.experimental.Accessors;
import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.direction.ReadingMode;

import java.awt.image.BufferedImage;
import java.io.File;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class ViewerFrame {
    private BufferedImage image;
    private String title;
    private File directory;
    private int firstPage;
    private int lastPage;
    private int pageCount;
    private boolean twoPageMode;
    private ReadingMode readingMode;
    private PageKey pageKey;

    @Builder
    public ViewerFrame(BufferedImage image, String title, File directory, int firstPage, int lastPage, int pageCount, boolean twoPageMode, ReadingMode readingMode, PageKey pageKey) {
        this.image = image;
        this.title = title;
        this.directory = directory;
        this.firstPage = firstPage;
        this.lastPage = lastPage;
        this.pageCount = pageCount;
        this.twoPageMode = twoPageMode;
        this.readingMode = readingMode;
        this.pageKey = pageKey;
    }

}
