package org.dustyroom.be.models.library;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@Setter
@Accessors(chain = true)
public final class LibraryWork {
    private String id;
    private String title;
    private String root;
    private long addedAt;
    private List<LibraryChapter> chapters;

    @Builder
    public LibraryWork(String id, String title, String root, long addedAt, List<LibraryChapter> chapters) {
        this.id = id;
        this.title = title;
        this.root = root;
        this.addedAt = addedAt;
        setChapters(chapters);
    }

    public LibraryChapter getResumeChapter() {
        LibraryChapter latest = chapters.getFirst();
        for (LibraryChapter chapter : chapters) {
            if (chapter.getLastRead() > latest.getLastRead()) {
                latest = chapter;
            }
        }
        return latest;
    }

    public long getCompleted() {
        return chapters
                .stream()
                .filter(LibraryChapter::isFinished)
                .count();
    }

    public long getLastRead() {
        return getResumeChapter().getLastRead();
    }

    public LibraryWork setChapters(List<LibraryChapter> chapters) {
        if (chapters.isEmpty()) {
            throw new IllegalArgumentException("A library work must contain a chapter.");
        }
        this.chapters = List.copyOf(chapters);
        return this;
    }
}
