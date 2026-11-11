package org.dustyroom.be.models.library;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class LibrarySnapshot {
    private List<LibraryWork> works;
    private Set<String> available;

    @Builder
    public LibrarySnapshot(List<LibraryWork> works, Set<String> available) {
        works = List.copyOf(works);
        available = Set.copyOf(available);

        this.works = works;
        this.available = available;
    }

    public LibrarySnapshot setWorks(List<LibraryWork> works) {
        this.works = List.copyOf(works);
        return this;
    }

    public LibrarySnapshot setAvailable(Set<String> available) {
        this.available = Set.copyOf(available);
        return this;
    }
}
