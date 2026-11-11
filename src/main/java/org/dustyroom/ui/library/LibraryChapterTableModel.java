package org.dustyroom.ui.library;

import org.dustyroom.be.models.library.LibraryChapter;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Set;

final class LibraryChapterTableModel extends AbstractTableModel {
    private final List<LibraryChapter> chapters;
    private final Set<String> available;

    LibraryChapterTableModel(List<LibraryChapter> chapters, Set<String> available) {
        this.chapters = chapters;
        this.available = available;
    }

    @Override
    public int getRowCount() {
        return chapters.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Chapter";
            case 1 -> "Progress";
            default -> "Source";
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        LibraryChapter chapter = chapters.get(row);
        return switch (column) {
            case 0 -> chapter.getSource().getTitle();
            case 1 -> chapter.getProgress();
            default -> available.contains(chapter.getId()) ? "Available" : "Unavailable";
        };
    }
}
