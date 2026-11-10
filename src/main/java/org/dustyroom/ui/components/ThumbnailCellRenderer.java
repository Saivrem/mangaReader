package org.dustyroom.ui.components;

import java.awt.Component;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ThumbnailCellRenderer extends DefaultListCellRenderer {
    private final Map<Integer, Icon> cache;
    private final IntSupplier first;
    private final IntSupplier last;
    private final BooleanSupplier gridMode;

    @Override
    public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean selected, boolean focus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index,
                        (index >= first.getAsInt() && index <= last.getAsInt()) || (gridMode.getAsBoolean() && selected), focus);
        Icon icon = cache.get(index);
        label.setText("Page " + (index + 1) + (icon == null ? " · …" : icon.getIconWidth() < 0 ? " · unavailable" : ""));
        label.setIcon(icon);
        label.setHorizontalAlignment(CENTER);
        label.setHorizontalTextPosition(CENTER);
        label.setVerticalTextPosition(BOTTOM);
        label.setIconTextGap(6);
        label.setBorder(BorderFactory.createCompoundBorder(
                        (focus || (gridMode.getAsBoolean() && selected)) ? BorderFactory.createLineBorder(owner.getSelectionBackground(), 2)
                                : BorderFactory.createEmptyBorder(2, 2, 2, 2),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        return label;
    }

}
