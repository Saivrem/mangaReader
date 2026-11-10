package org.dustyroom.be.models.library;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class LibraryImport {
    private String root;
    private String title;
    private List<LibrarySource> sources;
    private List<String> warnings;

    @Builder
    public LibraryImport(String root, String title, List<LibrarySource> sources, List<String> warnings) {
        sources = List.copyOf(sources);
        warnings = List.copyOf(warnings);

        this.root = root;
        this.title = title;
        this.sources = sources;
        this.warnings = warnings;
    }

    public LibraryImport setSources(List<LibrarySource> sources) {
        this.sources = List.copyOf(sources);
        return this;
    }

    public LibraryImport setWarnings(List<String> warnings) {
        this.warnings = List.copyOf(warnings);
        return this;
    }
}
