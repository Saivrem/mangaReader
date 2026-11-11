package org.dustyroom.ui.library;

import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySource;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

final class LibraryImportTableModel extends AbstractTableModel {
    private final List<LibraryImport> works;
    private final List<LibraryImport> owners = new ArrayList<>();
    private final List<LibrarySource> sources = new ArrayList<>();
    private final BitSet included = new BitSet();

    LibraryImportTableModel(List<LibraryImport> works) {
        this.works = works;
        for (LibraryImport work : works) {
            List<LibrarySource> chapters = work.getSources();
            boolean mixed = chapters.size() > 1 && chapters
                    .stream()
                    .anyMatch(LibrarySource::isDirectory);

            for (LibrarySource source : chapters) {
                owners.add(work);
                included.set(sources.size(), !mixed);
                sources.add(source);
            }
        }
    }

    @Override
    public int getRowCount() {
        return sources.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Import";
            case 1 -> "Book title";
            default -> "Chapter";
        };
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 0 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return switch (column) {
            case 0 -> included.get(row);
            case 1 -> owners.get(row).getTitle();
            default -> sources.get(row).toString();
        };
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 2;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column == 0) {
            included.set(row, Boolean.TRUE.equals(value));
            fireTableCellUpdated(row, column);
        } else if (column == 1) {
            owners.get(row).setTitle(value.toString().strip());
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

    List<LibraryImport> selection() {
        List<LibraryImport> selected = new ArrayList<>();
        int row = 0;
        for (LibraryImport work : works) {
            List<LibrarySource> chapters = new ArrayList<>();
            for (LibrarySource source : work.getSources()) {
                if (included.get(row)) {
                    chapters.add(source);
                }
                row++;
            }
            if (!chapters.isEmpty()) {
                selected.add(new LibraryImport(work.getRoot(), work.getTitle(), chapters));
            }
        }
        return selected;
    }
}
