package org.dustyroom.ui.components;

import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@RequiredArgsConstructor
final class ThumbnailCellRenderer extends DefaultListCellRenderer {
    private final Map<Integer, Icon> cache;
    private final IntSupplier first;
    private final IntSupplier last;
    private final BooleanSupplier gridMode;

    @Override
    public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean selected, boolean focus) {
        boolean gridSelection = gridMode.getAsBoolean() && selected;
        boolean visiblePage = index >= first.getAsInt() && index <= last.getAsInt();
        JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index,
                visiblePage || gridSelection, focus);
        Icon icon = cache.get(index);
        String title = "Page " + (index + 1);
        if (icon == null) {
            title += " · …";
        } else if (icon.getIconWidth() < 0) {
            title += " · unavailable";
        }

        label.setText(title);
        label.setIcon(icon);
        label.setHorizontalAlignment(CENTER);
        label.setHorizontalTextPosition(CENTER);
        label.setVerticalTextPosition(BOTTOM);
        label.setIconTextGap(6);
        label.setBorder(BorderFactory.createCompoundBorder(
                (focus || gridSelection) ? BorderFactory.createLineBorder(owner.getSelectionBackground(), 2)
                        : BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        return label;
    }

}
