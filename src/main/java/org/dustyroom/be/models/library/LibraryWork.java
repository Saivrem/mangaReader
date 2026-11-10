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
public final class LibraryWork {
    private String id;
    private String title;
    private String root;
    private long addedAt;
    private List<LibraryChapter> chapters;

    @Builder
    public LibraryWork(String id, String title, String root, long addedAt, List<LibraryChapter> chapters) {
        chapters = List.copyOf(chapters);
        if (chapters.isEmpty()) {
            throw new IllegalArgumentException("A library work must contain a chapter.");
        }

        this.id = id;
        this.title = title;
        this.root = root;
        this.addedAt = addedAt;
        this.chapters = chapters;
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
        return chapters.stream().filter(LibraryChapter::isFinished).count();
    }
    public long getLastRead() {
        return chapters.stream().mapToLong(LibraryChapter::getLastRead).max().orElse(0);
    }

    public LibraryWork setChapters(List<LibraryChapter> chapters) {
        if (chapters.isEmpty()) {
            throw new IllegalArgumentException("A library work must contain a chapter.");
        }
        this.chapters = List.copyOf(chapters);
        return this;
    }
}
