package org.dustyroom.ui.loading;

import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ImageIteratorFactory;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.persistence.BookState;
import org.dustyroom.ui.rendering.ThumbnailRenderer;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Filesystem checks and decoding belong on a worker, never on Swing's EDT.
 */
public final class RecentBookLoader {
    private RecentBookLoader() {
    }

    public static RecentBook inspect(BookState state) {
        boolean available;
        try {
            Path path = Path.of(state.sourceId());
            available = Files.isReadable(path) && (Files.isDirectory(path) || Files.isRegularFile(path));
        } catch (RuntimeException exception) {
            available = false;
        }
        return new RecentBook(state, available, null);
    }

    public static RecentBook cover(RecentBook book) {
        if (!book.available()) return book;
        try (ImageIterator source = ImageIteratorFactory.create(Path.of(book.state().sourceId()).toFile())) {
            List<PageRef> pages = source.pages();
            if (!pages.isEmpty()) {
                BufferedImage image = source.load(pages.get(0)).image();
                return new RecentBook(book.state(), true, ThumbnailRenderer.render(image), book.chapters());
            }
        } catch (RuntimeException ignored) {
            // A missing or corrupt preview must not hide the saved reading position.
        }
        RecentBook checked = inspect(book.state());
        return new RecentBook(book.state(), checked.available(), null, book.chapters());
    }
}
