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
    private final Map<String, BookCard> entries = new LinkedHashMap<>();
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
                BookCard card = new BookCard(book.state().sourceId());
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

    private final class BookCard extends JPanel {
        private final JLabel cover = new JLabel(new ReaderIcon(ReaderIcon.Kind.OPEN));
        private final JButton title = new JButton();
        private final JLabel progress = new JLabel();
        private final JLabel availability = new JLabel();
        private final JButton chapters = new JButton();
        private RecentBook current;
        private final JProgressBar bar = new JProgressBar(0, 100);

        BookCard(String id) {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            setPreferredSize(new Dimension(350, 176));
            cover.setPreferredSize(new Dimension(72, 104));
            cover.setHorizontalAlignment(SwingConstants.CENTER);
            add(cover, BorderLayout.WEST);
            JPanel details = new JPanel();
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            title.setHorizontalAlignment(SwingConstants.LEFT);
            title.putClientProperty("JButton.buttonType", "borderless");
            title.addActionListener(event -> open.accept(id));
            title.setToolTipText(id);
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
            JButton hide = new JButton("Remove from recent");
            hide.putClientProperty("JButton.buttonType", "borderless");
            hide.setToolTipText("Keep this book and its saved reading position");
            hide.getAccessibleContext().setAccessibleName("Remove " + new java.io.File(id).getName() + " from recent");
            hide.addActionListener(event -> remove.accept(id));
            chapters.putClientProperty("JButton.buttonType", "borderless");
            chapters.addActionListener(event -> {
                JPopupMenu menu = new JPopupMenu();
                for (var chapter : current.chapters().stream().limit(20).toList()) {
                    JMenuItem entry = new JMenuItem();
                    entry.putClientProperty("html.disable", true);
                    entry.setText(new java.io.File(chapter.sourceId()).getName() + " — " + chapter.pageNumber() + "/" + chapter.pageCount());
                    entry.setToolTipText(chapter.sourceId());
                    entry.addActionListener(click -> open.accept(chapter.sourceId()));
                    menu.add(entry);
                }
                menu.show(chapters, 0, chapters.getHeight());
            });
            for (JComponent item : new JComponent[]{title, progress, bar, chapters, availability, hide}) {
                item.setAlignmentX(LEFT_ALIGNMENT);
                details.add(item);
            }
            add(details, BorderLayout.CENTER);
        }

        void update(RecentBook book) {
            current = book;
            chapters.setVisible(book.chapters().size() > 1);
            chapters.setText("Chapters (" + book.chapters().size() + ") ▾");
            chapters.setToolTipText("Up to 20 recently read chapters; continue: " + new java.io.File(book.state().sourceId()).getName());
            // Treat filenames as plain text, never as Swing HTML markup.
            title.putClientProperty("html.disable", true);
            title.setText(book.title());
            title.setEnabled(book.available());
            title.getAccessibleContext().setAccessibleName("Continue " + book.title() + ", " + book.progress());
            progress.setText(book.progress());
            bar.setValue((int) (100L * book.state().pageNumber() / Math.max(1, book.state().pageCount())));
            availability.setText(book.available() ? " " : "Moved, missing or unavailable");
            availability.setToolTipText(book.state().sourceId());
            if (book.cover() != null) {
                var image = book.cover();
                cover.setIcon(new Icon() {
                    public int getIconWidth() {
                        return 72;
                    }

                    public int getIconHeight() {
                        return 104;
                    }

                    public void paintIcon(Component c, Graphics graphics, int x, int y) {
                        double scale = Math.min(72.0 / image.getWidth(), 104.0 / image.getHeight());
                        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
                        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
                        Graphics2D g = (Graphics2D) graphics.create();
                        try {
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.drawImage(image, x + (72 - w) / 2, y + (104 - h) / 2, w, h, null);
                        } finally {
                            g.dispose();
                        }
                    }
                });
            } else cover.setIcon(new ReaderIcon(ReaderIcon.Kind.OPEN));
        }
    }
}
