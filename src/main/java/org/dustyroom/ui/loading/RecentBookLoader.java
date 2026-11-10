package org.dustyroom.ui.loading;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ImageIteratorFactory;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.ui.rendering.ThumbnailRenderer;

/**
 * Filesystem checks and decoding belong on a worker, never on Swing's EDT.
 */
@UtilityClass
public class RecentBookLoader {

    public static RecentBook inspect(BookState state) {
        boolean available;
        try {
            Path path = Path.of(state.getSourceId());
            available = Files.isReadable(path) && (Files.isDirectory(path) || Files.isRegularFile(path));
        } catch (RuntimeException exception) {
            available = false;
        }
        return new RecentBook(state, available, null);
    }

    public static RecentBook cover(RecentBook book) {
        if (!book.isAvailable()) {
            return book;
        }
        try (ImageIterator source = ImageIteratorFactory.create(Path.of(book.getState().getSourceId()).toFile())) {
            List<PageRef> pages = source.pages();
            if (!pages.isEmpty()) {
                BufferedImage image = source.load(pages.get(0)).getImage();
                return new RecentBook(book.getState(), true, ThumbnailRenderer.render(image), book.getChapters());
            }
        } catch (RuntimeException ignored) {
            // A missing or corrupt preview must not hide the saved reading position.
        }
        RecentBook checked = inspect(book.getState());
        return new RecentBook(book.getState(), checked.isAvailable(), null, book.getChapters());
    }
}
