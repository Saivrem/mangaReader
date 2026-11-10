package org.dustyroom.ui.library;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibrarySnapshot;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.ui.components.ReaderIcon;
import org.dustyroom.ui.components.ReaderIconKind;
import org.dustyroom.ui.components.ThumbnailIcon;
import org.dustyroom.ui.listeners.DocumentChangeListener;
import org.dustyroom.ui.listeners.MouseClickListener;

public final class LibraryPanel extends JPanel {
    private static final int PAGE_SIZE = 12;
    private final LibraryController controller;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Unread", "Reading", "Finished", "Unavailable"});
    private final JComboBox<String> sort = new JComboBox<>(new String[]{"Title", "Last read", "Date added"});
    private final JLabel status = new JLabel("Add a folder to start your library. Your files stay where they are.");
    private LibrarySnapshot snapshot = new LibrarySnapshot(List.of(), Set.of());
    private String selectedWork;
    private String selectedChapter;
    private int page;

    LibraryPanel(LibraryController controller) {
        super(new BorderLayout(12, 12));
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING));
        controls.add(button("Add folder…", controller::chooseFolder));
        controls.add(button("Refresh", controller::refresh));
        controls.add(new JLabel("Search"));
        search.getAccessibleContext().setAccessibleName("Search library by title or path");
        controls.add(search);
        controls.add(filter);
        controls.add(sort);
        top.add(controls, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentChangeListener(this::changed));
        filter.addActionListener(event -> changed());
        sort.addActionListener(event -> changed());
        render();
    }

    private void changed() {
        selectedWork = null;
        page = 0;
        render();
    }
    void status(String text) {
        status.setText(text);
    }
    void showSnapshot(LibrarySnapshot snapshot) {
        this.snapshot = snapshot;
        status(snapshot.getWorks().size() + " works · Files and reading history are kept separately.");
        render();
    }

    private boolean matches(LibraryWork work) {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        if (!(work.getTitle() + " " + work.getRoot()).toLowerCase(Locale.ROOT).contains(query)) {
            return false;
        }
        return switch (filter.getSelectedIndex()) {
            case 1 -> work.getLastRead() == 0 && work.getCompleted() == 0;
            case 2 -> (work.getLastRead() > 0 || work.getCompleted() > 0) && work.getCompleted() < work.getChapters().size();
            case 3 -> !work.getChapters().isEmpty() && work.getCompleted() == work.getChapters().size();
            case 4 -> work.getChapters().stream().anyMatch(chapter -> !snapshot.getAvailable().contains(chapter.getId()));
            default -> true;
        };
    }

    private void render() {
        controller.invalidateCovers();
        body.removeAll();
        LibraryWork selected = snapshot.getWorks().stream().filter(work -> work.getId().equals(selectedWork)).findFirst().orElse(null);
        if (selected != null) {
            details(selected);
        } else {
            shelf();
        }
        body.revalidate();
        body.repaint();
    }

    private void shelf() {
        Comparator<LibraryWork> order = switch (sort.getSelectedIndex()) {
            case 1 -> Comparator.comparingLong(LibraryWork::getLastRead).reversed();
            case 2 -> Comparator.comparingLong(LibraryWork::getAddedAt).reversed();
            default -> Comparator.comparing(LibraryWork::getTitle, String.CASE_INSENSITIVE_ORDER);
        };
        List<LibraryWork> works = snapshot.getWorks().stream().filter(this::matches).sorted(order).toList();
        page = Math.min(page, Math.max(0, (works.size() - 1) / PAGE_SIZE));
        JPanel grid = new JPanel(new GridLayout(0, 3, 16, 16));
        for (LibraryWork work : works.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList()) {
            JPanel card = new JPanel(new BorderLayout(8, 8));
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            JButton cover = button(work.getTitle(), () -> {
                selectedWork = work.getId();
                render();
            });
            cover.setIcon(new ReaderIcon(ReaderIconKind.BOOK, 64));
            cover.setIconTextGap(16);
            cover.setPreferredSize(new Dimension(180, 230));
            cover.setVerticalTextPosition(SwingConstants.BOTTOM);
            cover.setHorizontalTextPosition(SwingConstants.CENTER);
            cover.setToolTipText(work.getRoot());
            card.add(cover, BorderLayout.CENTER);
            JPanel caption = new JPanel(new GridLayout(0, 1, 0, 4));
            caption.add(new JLabel(work.getChapters().size() + " chapters · " + work.getCompleted() + " finished"));
            LibraryChapter resume = work.getResumeChapter();
            JButton continueReading = button(resume.getLastRead() > 0 ? "Continue reading" : "Start reading", () -> controller.open(resume));
            continueReading.setEnabled(snapshot.getAvailable().contains(resume.getId()));
            caption.add(new JLabel(resume.getProgress()));
            caption.add(continueReading);
            boolean unavailable = work.getChapters().stream().anyMatch(chapter -> !snapshot.getAvailable().contains(chapter.getId()));
            caption.add(new JLabel(unavailable ? "Some sources are unavailable" : " "));
            card.add(caption, BorderLayout.SOUTH);
            grid.add(card);
            controller.cover(work.getChapters().getFirst(), image -> {
                if (image != null) {
                    cover.setIcon(new ThumbnailIcon(image));
                }
            });
        }
        if (works.isEmpty()) {
            body.add(new JLabel("No works found. Add a folder or change the filters.", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            JPanel aligned = new JPanel(new BorderLayout());
            aligned.add(grid, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(aligned);
            scroll.getVerticalScrollBar().setUnitIncrement(24);
            body.add(scroll, BorderLayout.CENTER);
        }
        JPanel paging = new JPanel();
        JButton previous = button("Previous", () -> {
            page--;
            render();
        });
        previous.setEnabled(page > 0);
        JButton next = button("Next", () -> {
            page++;
            render();
        });
        next.setEnabled((page + 1) * PAGE_SIZE < works.size());
        paging.add(previous);
        paging.add(new JLabel("Page " + (page + 1) + " / " + Math.max(1, (works.size() + PAGE_SIZE - 1) / PAGE_SIZE)));
        paging.add(next);
        body.add(paging, BorderLayout.SOUTH);
    }

    private void details(LibraryWork work) {
        JPanel heading = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        actions.add(button("← Library", () -> {
            selectedWork = null;
            render();
        }));
        actions.add(button("Rename…", () -> controller.rename(work)));
        actions.add(button("Remove from library…", () -> controller.remove(work)));
        heading.add(actions);
        heading.add(new JLabel(work.getTitle()));
        JTextField path = new JTextField(work.getRoot());
        path.setEditable(false);
        heading.add(path);
        heading.add(new JLabel("Unavailable sources retain their progress. Restore the original path and click Refresh."));
        body.add(heading, BorderLayout.NORTH);
        JTable chapters = new JTable(new LibraryChapterTableModel(work.getChapters(), snapshot.getAvailable()));
        chapters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chapters.setRowHeight(32);
        int selectedRow = 0;
        for (int row = 0; row < work.getChapters().size(); row++) {
            if (work.getChapters().get(row).getId().equals(selectedChapter)) {
                selectedRow = row;
            }
        }
        chapters.setRowSelectionInterval(selectedRow, selectedRow);
        chapters.getSelectionModel().addListSelectionListener(event -> {
            int row = chapters.getSelectedRow();
            if (row >= 0) {
                selectedChapter = work.getChapters().get(row).getId();
            }
        });
        Runnable open = () -> {
            if (chapters.getSelectedRow() >= 0) {
                controller.open(work.getChapters().get(chapters.getSelectedRow()));
            }
        };
        chapters.addMouseListener(new MouseClickListener(event -> {
            if (event.getClickCount() == 2 && chapters.rowAtPoint(event.getPoint()) >= 0 && SwingUtilities.isLeftMouseButton(event)) {
                open.run();
            }
        }));
        JScrollPane chapterScroll = new JScrollPane(chapters);
        chapterScroll.setColumnHeaderView(chapters.getTableHeader());
        body.add(chapterScroll, BorderLayout.CENTER);
        JPanel actionsBottom = new JPanel();
        actionsBottom.add(button("Open selected chapter", open));
        actionsBottom.add(button("Toggle finished", () -> {
            int row = chapters.getSelectedRow();
            if (row >= 0) {
                controller.finish(work.getChapters().get(row));
            }
        }));
        body.add(actionsBottom, BorderLayout.SOUTH);
    }

    private static JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        return button;
    }
}
