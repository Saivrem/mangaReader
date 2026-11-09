package org.dustyroom.persistence;

import org.dustyroom.ui.navigation.ReadingMode;

public record BookState(
        String sourceId,
        String pageId,
        int pageNumber,
        int pageCount,
        boolean twoPageMode,
        ReadingMode readingMode
) {
}
