package org.dustyroom.ui.components;

import org.dustyroom.persistence.BookState;
import org.dustyroom.ui.loading.RecentBook;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

final class RecentBookCard extends JPanel {
    private final JLabel cover = new JLabel(new ReaderIcon(ReaderIconKind.OPEN));
    private final JButton title = new JButton();
    private final JLabel progress = new JLabel();
    private final JLabel availability = new JLabel();
    private final JButton chapters = new JButton();
    private final JProgressBar bar = new JProgressBar(0, 100);
    private RecentBook current;

    RecentBookCard(String id, Consumer<String> open, Consumer<String> remove) {
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
        hide.getAccessibleContext().setAccessibleName("Remove " + new File(id).getName() + " from recent");
        hide.addActionListener(event -> remove.accept(id));
        chapters.putClientProperty("JButton.buttonType", "borderless");
        chapters.addActionListener(event -> {
            JPopupMenu menu = new JPopupMenu();
            for (BookState chapter : current.chapters().stream().limit(20).toList()) {
                JMenuItem entry = new JMenuItem();
                entry.putClientProperty("html.disable", true);
                entry.setText(new File(chapter.sourceId()).getName() + " — " + chapter.pageNumber() + "/" + chapter.pageCount());
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
        chapters.setToolTipText("Up to 20 recently read chapters; continue: " + new File(book.state().sourceId()).getName());
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
            cover.setIcon(new RecentCoverIcon(book.cover()));
        } else cover.setIcon(new ReaderIcon(ReaderIconKind.OPEN));
    }
}
