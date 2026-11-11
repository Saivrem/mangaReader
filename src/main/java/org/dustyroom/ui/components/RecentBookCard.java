package org.dustyroom.ui.components;

import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;

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
        title.addActionListener(_ -> open.accept(id));
        title.setToolTipText(id);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        JButton hide = new JButton("Remove from recent");
        hide.putClientProperty("JButton.buttonType", "borderless");
        hide.setToolTipText("Keep this book and its saved reading position");
        hide.getAccessibleContext().setAccessibleName("Remove " + new File(id).getName() + " from recent");
        hide.addActionListener(_ -> remove.accept(id));
        chapters.putClientProperty("JButton.buttonType", "borderless");
        chapters.addActionListener(_ -> {
            JPopupMenu menu = new JPopupMenu();
            for (BookState chapter : current.menuChapters()) {
                JMenuItem entry = new JMenuItem();
                entry.putClientProperty("html.disable", true);
                entry.setText(new File(chapter.getSourceId()).getName() + " — " + chapter.getPageNumber() + "/" + chapter.getPageCount());
                entry.setToolTipText(chapter.getSourceId());
                entry.addActionListener(_ -> open.accept(chapter.getSourceId()));
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
        chapters.setVisible(book.getChapters().size() > 1);
        chapters.setText("Chapters (" + book.getChapters().size() + ") ▾");
        chapters.setToolTipText("Up to 20 recently read chapters; continue: " + new File(book.getState().getSourceId()).getName());
        // Treat filenames as plain text, never as Swing HTML markup.
        title.putClientProperty("html.disable", true);
        title.setText(book.getTitle());
        title.setEnabled(book.isAvailable());
        title.getAccessibleContext().setAccessibleName("Continue " + book.getTitle() + ", " + book.getProgress());
        progress.setText(book.getProgress());
        bar.setValue((int) (100L * book.getState().getPageNumber() / Math.max(1, book.getState().getPageCount())));
        availability.setText(book.isAvailable() ? " " : "Moved, missing or unavailable");
        availability.setToolTipText(book.getState().getSourceId());
        if (book.getCover() != null) {
            cover.setIcon(new RecentCoverIcon(book.getCover()));
        } else {
            cover.setIcon(new ReaderIcon(ReaderIconKind.OPEN));
        }
    }
}
