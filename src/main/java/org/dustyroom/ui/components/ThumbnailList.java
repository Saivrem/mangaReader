package org.dustyroom.ui.components;

import javax.swing.JList;
import org.dustyroom.be.models.book.PageRef;

/** Keeps wrap geometry current before scrolling and scheduling visible thumbnail requests. */
final class ThumbnailList extends JList<PageRef> {
    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean widthChanged = width != getWidth();
        super.setBounds(x, y, width, height);
        if (widthChanged && getLayoutOrientation() == HORIZONTAL_WRAP) {
            int columns = Math.max(1, width / 160);
            setFixedCellWidth(Math.max(160, width / columns));
            // BasicListUI otherwise notices the new wrap width only during paint.
            // Invalidate through the public layout property before querying cell bounds.
            setLayoutOrientation(VERTICAL);
            setLayoutOrientation(HORIZONTAL_WRAP);
        }
    }
}
