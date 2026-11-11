package org.dustyroom.be.models.library;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class LibraryChapter {
    private String id;
    private LibrarySource source;
    private boolean finished;
    private int page;
    private int pageCount;
    private long lastRead;

    @Builder
    public LibraryChapter(String id, LibrarySource source, boolean finished, int page, int pageCount, long lastRead) {
        this.id = id;
        this.source = source;
        this.finished = finished;
        this.page = page;
        this.pageCount = pageCount;
        this.lastRead = lastRead;
    }

    public String getProgress() {
        if (finished) {
            return "Finished";
        }

        return pageCount > 0 ? "Page " + page + " / " + pageCount : "Unread";
    }
}
