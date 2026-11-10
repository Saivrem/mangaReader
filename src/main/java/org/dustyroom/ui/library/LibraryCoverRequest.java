package org.dustyroom.ui.library;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.dustyroom.be.models.library.LibraryChapter;

@Getter
@EqualsAndHashCode
@ToString
final class LibraryCoverRequest {
    private final long generation;
    private final LibraryChapter chapter;
    private final Consumer<BufferedImage> loaded;

    @Builder
    public LibraryCoverRequest(long generation, LibraryChapter chapter, Consumer<BufferedImage> loaded) {
        this.generation = generation;
        this.chapter = chapter;
        this.loaded = loaded;
    }
}
