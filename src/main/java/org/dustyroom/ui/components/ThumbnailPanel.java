package org.dustyroom.ui.components;

import org.dustyroom.be.models.PageRef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * EDT-owned, virtualized list. Only one thumbnail request is outstanding at a time.
 */
public final class ThumbnailPanel extends JPanel {
    private static final int PREFETCH_MARGIN = 10;
    private final JList<PageRef> list = new JList<>();
    private final Loader loader;
    private final JPanel header = new JPanel(new BorderLayout());
    private final JScrollPane scroll = new JScrollPane(list);
    private final JButton handle = new JButton() {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = getModel().isRollover() || isFocusOwner();
                Color color = UIManager.getColor(active ? "Component.accentColor" : "Label.disabledForeground");
                g.setColor(color == null ? getForeground() : color);
                int width = active ? 5 : 3;
                g.fillRoundRect((getWidth() - width) / 2, (getHeight() - 32) / 2, width, 32, width, width);
            } finally {
                g.dispose();
            }
        }
    };
    private final Map<Integer, Icon> cache = new HashMap<>();
    private boolean expanded;
    private List<PageRef> pages = List.of();
    private int windowStart, windowEnd = -1;
    private boolean pending;
    private int first = -1, last = -1;
    public ThumbnailPanel(Loader loader, IntConsumer navigate, Runnable hide) {
        super(new BorderLayout());
        this.loader = loader;

        JLabel title = new JLabel("Pages");
        title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 0));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);
        list.setFixedCellHeight(228);
        list.setFixedCellWidth(160);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.getAccessibleContext().setAccessibleName("Book pages");
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index,
                        index >= first && index <= last, focus);
                Icon icon = cache.get(index);
                label.setText("Page " + (index + 1) + (icon == null ? " · …" : icon.getIconWidth() < 0 ? " · unavailable" : ""));
                label.setIcon(icon);
                label.setHorizontalAlignment(CENTER);
                label.setHorizontalTextPosition(CENTER);
                label.setVerticalTextPosition(BOTTOM);
                label.setIconTextGap(6);
                label.setBorder(BorderFactory.createCompoundBorder(
                        focus ? BorderFactory.createLineBorder(owner.getSelectionBackground(), 2)
                                : BorderFactory.createEmptyBorder(2, 2, 2, 2),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)));
                return label;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && index >= 0 && list.getCellBounds(index, index).contains(e.getPoint()))
                    navigate.accept(index + 1);
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openPage");
        list.getActionMap().put("openPage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedIndex() >= 0) navigate.accept(list.getSelectedIndex() + 1);
            }
        });

        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().addChangeListener(e -> pump());
        add(scroll, BorderLayout.CENTER);
        JPanel rail = new JPanel(new GridBagLayout());
        rail.setPreferredSize(new Dimension(16, 0));
        handle.setPreferredSize(new Dimension(16, 72));
        handle.setBorderPainted(false);
        handle.setContentAreaFilled(false);
        handle.setFocusPainted(false);
        handle.setRolloverEnabled(true);
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        handle.addActionListener(e -> hide.run());
        rail.add(handle);
        add(rail, BorderLayout.EAST);
        setExpanded(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                followCurrent();
                pump();
            }
        });
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        header.setVisible(expanded);
        scroll.setVisible(expanded);
        setPreferredSize(new Dimension(expanded ? 200 : 16, 0));
        String label = expanded ? "Hide thumbnails" : "Show thumbnails";
        handle.setToolTipText(label + " (T)");
        handle.getAccessibleContext().setAccessibleName(label);
        revalidate();
        repaint();
        if (expanded) SwingUtilities.invokeLater(() -> {
            followCurrent();
            pump();
        });
    }

    public void showPages(List<PageRef> newPages) {
        if (pages == newPages) return;
        pages = newPages;
        cache.clear();
        first = last = -1;
        list.setModel(new AbstractListModel<>() {
            @Override
            public int getSize() {
                return newPages.size();
            }

            @Override
            public PageRef getElementAt(int index) {
                return newPages.get(index);
            }
        });
        pump();
    }

    public void showSpread(int firstPage, int lastPage) {
        boolean changed = first != firstPage - 1 || last != lastPage - 1;
        first = firstPage - 1;
        last = lastPage - 1;
        if (changed) {
            list.setSelectedIndex(first);
            followCurrent();
        }
        list.repaint();
        pump();
    }

    private void followCurrent() {
        if (expanded && isVisible() && first >= 0) {
            Rectangle spread = list.getCellBounds(first, last);
            if (spread != null && spread.height <= list.getVisibleRect().height) list.scrollRectToVisible(spread);
            else list.ensureIndexIsVisible(first);
        }
    }

    private void pump() {
        if (!expanded || !isShowing() || pages.isEmpty()) return;
        int start = list.getFirstVisibleIndex(), end = list.getLastVisibleIndex();
        if (start < 0 || end < start) return;
        windowStart = Math.max(0, start - PREFETCH_MARGIN);
        windowEnd = Math.min(pages.size() - 1, end + PREFETCH_MARGIN);
        cache.keySet().removeIf(index -> index < windowStart || index > windowEnd);
        if (pending) return;
        // Visible pages first, then alternate forward/backward by distance from the viewport.
        for (int index = start; index <= end; index++) {
            if (request(index)) return;
        }
        for (int distance = 1; distance <= PREFETCH_MARGIN; distance++) {
            if (end + distance <= windowEnd && request(end + distance)) return;
            if (start - distance >= windowStart && request(start - distance)) return;
        }
    }

    private boolean request(int index) {
        if (cache.containsKey(index)) return false;
        int target = index;
        List<PageRef> captured = pages;
        pending = true;
        loader.load(index, pages.get(index), image -> {
            pending = false;
            if (pages == captured && target >= windowStart && target <= windowEnd) {
                // Empty icon marks an unreadable page; retry only if it leaves and re-enters the window.
                cache.put(target, image == null ? new ImageIcon() : new ThumbnailIcon(image));
                list.repaint(list.getCellBounds(target, target));
            }
            pump();
        });
        return true;
    }

    @FunctionalInterface
    public interface Loader {
        void load(int index, PageRef page, Consumer<BufferedImage> result);
    }
}
