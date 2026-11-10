package org.dustyroom.be.library;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipFile;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.utils.FileUtils;
import org.dustyroom.be.utils.NaturalOrderComparator;

/** Explicit, shallow import. No image decoding, recursion or symbolic-link traversal. */
public final class LibraryScanner {
    public LibraryImport scan(Path directory, BooleanSupplier cancelled) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IOException("Select a real folder, not a symbolic link.");
        }
        List<LibrarySource> sources = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean images = false;
        try (DirectoryStream<Path> children = Files.newDirectoryStream(root)) {
            for (Path child : children) {
                checkCancelled(cancelled);
                if (!Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String name = child.getFileName().toString();
                if (FileUtils.isSupportedImage(name)) {
                    images = true;
                }
                if (!FileUtils.isSupportedArchive(name)) {
                    continue;
                }
                try (ZipFile zip = new ZipFile(child.toFile())) {
                    boolean hasImages = zip.stream().anyMatch(entry -> {
                        checkCancelled(cancelled);
                        return !entry.isDirectory() && FileUtils.isSupportedImage(entry.getName());
                    });
                    if (hasImages) {
                        sources.add(new LibrarySource(child.toString(), name, false));
                    } else {
                        warnings.add(name + ": no supported pages");
                    }
                } catch (IOException exception) {
                    warnings.add(name + ": unreadable ZIP");
                }
            }
        }
        sources.sort((left, right) -> NaturalOrderComparator.INSTANCE.compare(left.getTitle(), right.getTitle()));
        String title = root.getFileName() == null ? root.toString() : root.getFileName().toString();
        if (images) {
            sources.addFirst(new LibrarySource(root.toString(), title, true));
        }
        return new LibraryImport(root.toString(), title, sources, warnings);
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }
}
