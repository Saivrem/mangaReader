package org.dustyroom.ui.loading;

import org.dustyroom.persistence.BookState;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Immutable snapshot shared by the welcome view and Open Recent menu.
 */
public record RecentBook(BookState state, boolean available, BufferedImage cover) {
    public String title() {
        String name = new File(state.sourceId()).getName();
        return name.isBlank() ? state.sourceId() : name;
    }

    public String progress() {
        return "Page " + state.pageNumber() + " / " + state.pageCount();
    }
}
