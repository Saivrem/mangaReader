package org.dustyroom.ui.loading;

import org.dustyroom.persistence.BookState;

import java.nio.file.Path;
import java.util.*;

/**
 * Explicit folder grouping, not guessing series names from filenames.
 */
public final class RecentGroups {
    private RecentGroups() {
    }

    public static List<List<BookState>> select(List<BookState> history, boolean grouped, int limit) {
        Map<String, List<BookState>> groups = new LinkedHashMap<String, List<BookState>>();
        for (BookState book : history) {
            String key = "book:" + book.sourceId();
            if (grouped && book.sourceId().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                Path parent = Path.of(book.sourceId()).getParent();
                if (parent != null) key = "folder:" + parent;
            }
            if (groups.containsKey(key)) groups.get(key).add(book);
            else if (groups.size() < limit) {
                List<BookState> books = new ArrayList<BookState>();
                books.add(book);
                groups.put(key, books);
            }
        }
        return groups.values().stream().map(List::copyOf).toList();
    }
}
