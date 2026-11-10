package org.dustyroom.ui.components;

import java.util.List;
import javax.swing.AbstractListModel;
import lombok.RequiredArgsConstructor;
import org.dustyroom.be.models.book.PageRef;

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
