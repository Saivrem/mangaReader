package org.dustyroom.be.models.library;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class LibraryImport {
    private String root;
    private String title;
    private List<LibrarySource> sources;

    @Builder
    public LibraryImport(String root, String title, List<LibrarySource> sources) {
        this.root = root;
        this.title = title;
        this.sources = List.copyOf(sources);
    }

    public LibraryImport setSources(List<LibrarySource> sources) {
        this.sources = List.copyOf(sources);
        return this;
    }
}
