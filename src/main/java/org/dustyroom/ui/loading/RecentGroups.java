package org.dustyroom.ui.loading;

import lombok.experimental.UtilityClass;
import org.dustyroom.be.models.state.BookState;

import java.nio.file.Path;
import java.util.*;

/**
 * Explicit folder grouping, not guessing series names from filenames.
 */
@UtilityClass
public class RecentGroups {

    public static List<List<BookState>> select(List<BookState> history, boolean grouped, int limit) {
        Map<String, List<BookState>> groups = new LinkedHashMap<>();
        for (BookState book : history) {
            String key = "book:" + book.getSourceId();
            if (grouped && book.getSourceId().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                Path parent = Path.of(book.getSourceId()).getParent();
                if (parent != null) {
                    key = "folder:" + parent;
                }
            }
            if (groups.containsKey(key)) {
                groups.get(key).add(book);
            } else if (groups.size() < limit) {
                List<BookState> books = new ArrayList<>();
                books.add(book);
                groups.put(key, books);
            }
        }
        return groups.values()
                .stream()
                .map(List::copyOf)
                .toList();
    }
}
