package org.dustyroom.ui.components;

import org.dustyroom.ui.loading.RecentBook;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Small, bounded shelf; cover updates preserve keyboard focus on existing cards.
 */
public final class RecentBooksPanel extends JPanel {
    private final JCheckBox groupByFolder = new JCheckBox("Group ZIPs by folder", true);
    private final JPanel cards = new JPanel(new GridLayout(0, 2, 16, 16));
    private final JLabel empty = new JLabel("Your recently read books will appear here.");
    private final Map<String, RecentBookCard> entries = new LinkedHashMap<>();
    private Consumer<String> open = id -> {
    };
    private Consumer<String> remove = id -> {
    };

    public RecentBooksPanel() {
        super(new BorderLayout(0, 14));
        JLabel heading = new JLabel("Recently read");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JPanel header = new JPanel(new BorderLayout());
        header.add(heading, BorderLayout.WEST);
        header.add(groupByFolder, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(empty, BorderLayout.CENTER);
        setMaximumSize(new Dimension(760, Integer.MAX_VALUE));
    }

    public void setGrouping(boolean grouped, Consumer<Boolean> change) {
        groupByFolder.setSelected(grouped);
        groupByFolder.addActionListener(event -> change.accept(groupByFolder.isSelected()));
    }

    public void setHandlers(Consumer<String> open, Consumer<String> remove) {
        this.open = open;
        this.remove = remove;
    }

    public void showBooks(List<RecentBook> books) {
        List<String> ids = books.stream().map(book -> book.state().sourceId()).toList();
        if (!ids.equals(List.copyOf(entries.keySet()))) {
            entries.clear();
            cards.removeAll();
            for (RecentBook book : books) {
                RecentBookCard card = new RecentBookCard(book.state().sourceId(), id -> open.accept(id), id -> remove.accept(id));
                entries.put(book.state().sourceId(), card);
                cards.add(card);
            }
        }
        for (RecentBook book : books) entries.get(book.state().sourceId()).update(book);
        remove(empty);
        remove(cards);
        add(books.isEmpty() ? empty : cards, BorderLayout.CENTER);
        revalidate();
        repaint();
    }


}
