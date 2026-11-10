package org.dustyroom.be.models.library;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@EqualsAndHashCode
@Setter
@Accessors(chain = true)
public final class LibrarySource {
    private String path;
    private String title;
    private boolean directory;

    @Builder
    public LibrarySource(String path, String title, boolean directory) {
        this.path = path;
        this.title = title;
        this.directory = directory;
    }

    @Override
    public String toString() {
        return title + (directory ? " (image folder)" : "");
    }
}
