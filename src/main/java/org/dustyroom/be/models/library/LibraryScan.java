package org.dustyroom.be.models.library;

import java.util.List;

public record LibraryScan(List<LibraryImport> works, List<String> warnings) {
    public LibraryScan(List<LibraryImport> works, List<String> warnings) {
        this.works = List.copyOf(works);
        this.warnings = List.copyOf(warnings);
    }
}
