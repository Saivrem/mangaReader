package org.dustyroom.ui.components;

import lombok.Getter;
import lombok.Setter;

import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.ui.actions.RunnableAction;
import org.dustyroom.ui.listeners.ComponentResizeListener;
import org.dustyroom.ui.listeners.ComponentShownListener;
import org.dustyroom.ui.listeners.MouseClickListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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
    private final JList<PageRef> list = new ThumbnailList();
    private final ThumbnailLoader loader;
    private final JScrollPane scroll = new JScrollPane(list);
    private final JButton handle = new ThumbnailHandle();
    private final Map<Integer, Icon> cache = new HashMap<>();
    private final JPanel rail = new JPanel(new GridBagLayout());
    private final JButton gridButton = new JButton(new ReaderIcon(ReaderIconKind.PAGE_GRID, 16));
    private final IntConsumer navigate;
    @Setter
    private Consumer<Boolean> gridChanged = _ -> {
    };
    private boolean fullscreen;
    @Getter
    private boolean gridMode;
    private boolean expanded;
    private List<PageRef> pages = List.of();
    private int windowStart, windowEnd = -1;
    private boolean pending;
    private int first = -1, last = -1;

    public ThumbnailPanel(ThumbnailLoader loader, IntConsumer navigate, Runnable hide) {
        super(new BorderLayout());
        this.loader = loader;
        this.navigate = navigate;

        gridButton.setToolTipText("Expand page grid");
        gridButton.getAccessibleContext().setAccessibleName("Expand page grid");
        gridButton.addActionListener(_ -> setGridMode(!gridMode));
        gridButton.setEnabled(false);
        gridButton.putClientProperty("JButton.buttonType", "toolBarButton");
        gridButton.setVisible(false);
        list.setFixedCellHeight(228);
        list.setFixedCellWidth(160);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.getAccessibleContext().setAccessibleName("Book pages");
        list.setCellRenderer(new ThumbnailCellRenderer(cache, () -> first, () -> last, () -> gridMode));
        list.addMouseListener(new MouseClickListener(e -> {
            int index = list.locationToIndex(e.getPoint());
            if (SwingUtilities.isLeftMouseButton(e) && index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
                openPage(index);
            }
        }));
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openPage");
        list.getActionMap().put("openPage", new RunnableAction("Open page", () -> {
            if (list.getSelectedIndex() >= 0) {
                openPage(list.getSelectedIndex());
            }
        }));

        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().addChangeListener(_ -> pump());
        scroll.getViewport().addComponentListener(new ComponentResizeListener(this::pump));
        getActionMap().put("closeGrid", new RunnableAction("Return to reader", () -> setGridMode(false)));
        add(scroll, BorderLayout.CENTER);
        rail.setPreferredSize(new Dimension(16, 0));
        handle.setPreferredSize(new Dimension(16, 72));
        handle.setBorderPainted(false);
        handle.setContentAreaFilled(false);
        handle.setFocusPainted(false);
        handle.setRolloverEnabled(true);
        handle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        handle.addActionListener(_ -> hide.run());
        rail.add(handle);
        add(rail, BorderLayout.EAST);
        add(gridButton, BorderLayout.SOUTH);
        setExpanded(true);
        addComponentListener(new ComponentShownListener(() -> {
            followCurrent();
            pump();
        }));
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        updateGridButton();
    }

    private void updateGridButton() {
        gridButton.setVisible(gridMode || (fullscreen && expanded));
    }

    public void setGridMode(boolean gridMode) {
        if (this.gridMode == gridMode || (gridMode && pages.isEmpty())) {
            return;
        }
        this.gridMode = gridMode;
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        if (gridMode) {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escape, "closeGrid");
        } else {
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(escape);
        }
        if (gridMode) {
            setExpanded(true);
        }
        list.setLayoutOrientation(gridMode ? JList.HORIZONTAL_WRAP : JList.VERTICAL);
        list.setVisibleRowCount(gridMode ? -1 : 8);
        if (!gridMode) {
            list.setFixedCellWidth(160);
        }
        rail.setVisible(!gridMode);
        gridButton.setText(gridMode ? "Back to reader" : null);
        gridButton.setIcon(new ReaderIcon(gridMode ? ReaderIconKind.PREVIOUS : ReaderIconKind.PAGE_GRID, 16));
        gridButton.setToolTipText(gridMode ? "Return to reader (Esc)" : "Expand page grid");
        gridButton.getAccessibleContext().setAccessibleName(gridButton.getToolTipText());
        updateGridButton();
        gridChanged.accept(gridMode);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            followCurrent();
            pump();
            if (this.gridMode) {
                list.requestFocusInWindow();
            }
        });
    }

    private void openPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        setGridMode(false);
        navigate.accept(index + 1);
    }

    public void setExpanded(boolean expanded) {
        if (!expanded) {
            setGridMode(false);
        }
        if (this.expanded == expanded) {
            return;
        }
        this.expanded = expanded;
        updateGridButton();
        scroll.setVisible(expanded);
        setPreferredSize(new Dimension(expanded ? 200 : 16, 0));
        String label = expanded ? "Hide thumbnails" : "Show thumbnails";
        handle.setToolTipText(label + " (T)");
        handle.getAccessibleContext().setAccessibleName(label);
        revalidate();
        repaint();
        if (expanded) {
            SwingUtilities.invokeLater(() -> {
                followCurrent();
                pump();
            });
        }
    }

    public void showPages(List<PageRef> newPages) {
        if (pages == newPages) {
            return;
        }
        setGridMode(false);
        pages = newPages;
        gridButton.setEnabled(!newPages.isEmpty());
        cache.clear();
        first = last = -1;
        list.setModel(new PageListModel(List.copyOf(newPages)));
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
            if (spread != null && spread.height <= list.getVisibleRect().height) {
                list.scrollRectToVisible(spread);
            } else {
                list.ensureIndexIsVisible(first);
            }
        }
    }

    private void pump() {
        if (!expanded || !isShowing() || pages.isEmpty()) {
            return;
        }
        int start = list.getFirstVisibleIndex(), end = list.getLastVisibleIndex();
        if (start < 0 || end < start) {
            return;
        }
        windowStart = Math.max(0, start - PREFETCH_MARGIN);
        windowEnd = Math.min(pages.size() - 1, end + PREFETCH_MARGIN);
        cache.keySet().removeIf(index -> index < windowStart || index > windowEnd);
        if (pending) {
            return;
        }
        // Visible pages first, then alternate forward/backward by distance from the viewport.
        for (int index = start; index <= end; index++) {
            if (request(index)) {
                return;
            }
        }
        for (int distance = 1; distance <= PREFETCH_MARGIN; distance++) {
            if (end + distance <= windowEnd && request(end + distance)) {
                return;
            }
            if (start - distance >= windowStart && request(start - distance)) {
                return;
            }
        }
    }

    private boolean request(int index) {
        if (cache.containsKey(index)) {
            return false;
        }
        List<PageRef> captured = pages;
        pending = true;
        loader.load(index, pages.get(index), image -> {
            pending = false;
            if (pages == captured && index >= windowStart && index <= windowEnd) {
                // Empty icon marks an unreadable page; retry only if it leaves and re-enters the window.
                cache.put(index, image == null ? new ImageIcon() : new ThumbnailIcon(image));
                list.repaint(list.getCellBounds(index, index));
            }
            pump();
        });
        return true;
    }
}
