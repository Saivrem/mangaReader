package org.dustyroom.be.library;

import lombok.experimental.UtilityClass;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibrarySnapshot;
import org.dustyroom.be.models.library.LibraryWork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * Filesystem checks belong on a worker, never in a DTO or on the Swing EDT.
 */
@UtilityClass
public class LibraryInspector {
    public static LibrarySnapshot inspect(List<LibraryWork> works) {
        Set<String> available = new HashSet<>();
        for (LibraryWork work : works) {
            for (LibraryChapter chapter : work.getChapters()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException();
                }
                Path path = Path.of(chapter.getSource().getPath());
                boolean correctType = chapter.getSource().isDirectory()
                        ? Files.isDirectory(path) : Files.isRegularFile(path);
                if (Files.isReadable(path) && correctType) {
                    available.add(chapter.getId());
                }
            }
        }
        return new LibrarySnapshot(works, available);
    }
}
