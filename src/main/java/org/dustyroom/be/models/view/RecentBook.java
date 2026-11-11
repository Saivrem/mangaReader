package org.dustyroom.be.models.view;

import lombok.*;
import lombok.experimental.Accessors;
import org.dustyroom.be.models.state.BookState;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Immutable snapshot shared by the welcome view and Open Recent menu.
 */
@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class RecentBook {
    private BookState state;
    private boolean available;
    private BufferedImage cover;
    private List<BookState> chapters;

    @Builder
    public RecentBook(BookState state, boolean available, BufferedImage cover, List<BookState> chapters) {
        chapters = List.copyOf(chapters);

        this.state = state;
        this.available = available;
        this.cover = cover;
        this.chapters = chapters;
    }

    public RecentBook(BookState state, boolean available, BufferedImage cover) {
        this(state, available, cover, List.of(state));
    }

    public String getTitle() {
        File file = new File(state.getSourceId());
        File parent = file.getParentFile();
        if (chapters.size() > 1 && parent != null) {
            file = parent;
        }

        String name = file.getName();
        return name.isBlank() ? state.getSourceId() : name;
    }

    public List<BookState> menuChapters() {
        return chapters.subList(0, Math.min(chapters.size(), 20));
    }

    public String getProgress() {
        return "Page " + state.getPageNumber() + " / " + state.getPageCount();
    }

    public RecentBook setChapters(List<BookState> chapters) {
        this.chapters = List.copyOf(chapters);
        return this;
    }
}
