package org.dustyroom.ui.loading;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentGroupsTest {
    @Test
    void manyChaptersDoNotCrowdOutOtherSeriesAndUngroupingPreservesOrder() {
        List<BookState> books = new ArrayList<>();
        for (int i = 0; i < 40; i++) books.add(book("series-a", "chapter-" + i + ".zip"));
        books.add(book("series-b", "chapter-1.zip"));
        books.add(book("series-a", "images"));
        List<List<BookState>> groups = RecentGroups.select(books, true, 12);
        assertEquals(3, groups.size());
        assertEquals(40, groups.getFirst().size());
        assertEquals(books.getFirst(), groups.getFirst().getFirst());
        assertEquals(books.get(40), groups.get(1).getFirst());
        assertEquals(12, RecentGroups.select(books, false, 12).size());
        assertTrue(RecentGroups.select(books, false, 12)
                .stream()
                .allMatch(group -> group.size() == 1));
    }

    private BookState book(String directory, String file) {
        return new BookState(Path.of(directory, file).toString(), "page", 2, 10, false, ReadingMode.MANGA);
    }
}
