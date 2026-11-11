package org.dustyroom.be.models.state;

import lombok.*;
import lombok.experimental.Accessors;
import org.dustyroom.be.models.direction.ReadingMode;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class BookState {
    private String sourceId;
    private String pageId;
    private int pageNumber;
    private int pageCount;
    private boolean twoPageMode;
    private ReadingMode readingMode;

    @Builder
    public BookState(String sourceId, String pageId, int pageNumber, int pageCount, boolean twoPageMode, ReadingMode readingMode) {
        this.sourceId = sourceId;
        this.pageId = pageId;
        this.pageNumber = pageNumber;
        this.pageCount = pageCount;
        this.twoPageMode = twoPageMode;
        this.readingMode = readingMode;
    }

}
