package org.dustyroom.ui.loading;

import org.dustyroom.persistence.BookState;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Immutable snapshot shared by the welcome view and Open Recent menu.
 */
public record RecentBook(BookState state, boolean available, BufferedImage cover, java.util.List<BookState> chapters) {
    public RecentBook {
        chapters = java.util.List.copyOf(chapters);
    }

    public RecentBook(BookState state, boolean available, BufferedImage cover) {
        this(state, available, cover, java.util.List.of(state));
    }

    public String title() {
        File file = new File(state.sourceId());
        String name = chapters.size() > 1 && file.getParentFile() != null ? file.getParentFile().getName() : file.getName();
        return name.isBlank() ? state.sourceId() : name;
    }

    public String progress() {
        return "Page " + state.pageNumber() + " / " + state.pageCount();
    }
}
