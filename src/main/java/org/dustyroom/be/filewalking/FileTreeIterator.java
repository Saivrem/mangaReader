package org.dustyroom.be.filewalking;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class FileTreeIterator {

    private final Map<Path, List<Path>> tree;
    private final List<Path> treeKeys;
    private int treeIndex;
    private ListIterator<Path> pagesIterator;

    public FileTreeIterator(Map<Path, List<Path>> tree, Path file) {
        Path currentKey = file.getParent();
        List<Path> paths = tree.get(currentKey);
        int index = paths.indexOf(file);

        this.tree = tree;
        this.treeKeys = tree.keySet().stream().toList();
        this.treeIndex = treeKeys.indexOf(currentKey);
        this.pagesIterator = paths.listIterator(index == 0 ? 0 : index - 1);
    }

    public boolean hasNext() {
        return pagesIterator.hasNext() || treeIndex < treeKeys.size() - 1;
    }

    public boolean hasPrevious() {
        return pagesIterator.hasPrevious() || treeIndex > 0;
    }

    public Path next() {
        if (hasNext() && !pagesIterator.hasNext()) {
            Path path = treeKeys.get(++treeIndex);
            List<Path> paths = tree.get(path);
            pagesIterator = paths.listIterator();
        }
        try {
            return pagesIterator.next();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "There are no more images", "Info", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }

    public Path previous() {
        if (hasPrevious() && !pagesIterator.hasPrevious()) {
            Path path = treeKeys.get(--treeIndex);
            List<Path> paths = tree.get(path);
            pagesIterator = paths.listIterator(paths.size());
        }
        try {
            return pagesIterator.previous();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "There are no previous images", "Info", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }
}
