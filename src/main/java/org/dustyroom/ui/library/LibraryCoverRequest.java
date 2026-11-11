package org.dustyroom.ui.library;

import lombok.Builder;
import org.dustyroom.be.models.library.LibraryChapter;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

record LibraryCoverRequest(long generation, LibraryChapter chapter, Consumer<BufferedImage> loaded) {
    @Builder
    LibraryCoverRequest {
    }
}
