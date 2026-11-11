package org.dustyroom.ui.components;

import lombok.RequiredArgsConstructor;
import org.dustyroom.be.models.book.PageRef;

import javax.swing.*;
import java.util.List;

@RequiredArgsConstructor
final class PageListModel extends AbstractListModel<PageRef> {
    private final List<PageRef> newPages;

    @Override
    public int getSize() {
        return newPages.size();
    }

    @Override
    public PageRef getElementAt(int index) {
        return newPages.get(index);
    }

}
