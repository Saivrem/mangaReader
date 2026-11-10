package org.dustyroom.ui.library;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import org.dustyroom.be.library.LibraryInspector;
import org.dustyroom.be.library.LibraryScanner;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibraryImport;
import org.dustyroom.be.models.library.LibrarySnapshot;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.BackgroundTask;
import org.dustyroom.ui.loading.RecentBookLoader;

/** UI orchestration only. Import scanning and persistence never run on the EDT. */
public final class LibraryController {
    private final AsyncViewerSession session;
    private final Consumer<File> openBook;
    private final AtomicLong coverGeneration = new AtomicLong();
    private final LibraryPanel panel;
    private final Deque<LibraryCoverRequest> covers = new ArrayDeque<>();
    private boolean coverPending;
    private long request;
    private boolean importing;
    private SwingWorker<LibrarySnapshot, Void> availability;

    public LibraryController(AsyncViewerSession session, Consumer<File> openBook) {
        this.session = session;
        this.openBook = openBook;
        panel = new LibraryPanel(this);
    }

    public LibraryPanel panel() {
        return panel;
    }
    public void refresh() {
        long current = ++request;
        if (availability != null) {
            availability.cancel(true);
        }
        invalidateCovers();
        panel.status("Checking library…");
        session.libraryTask(ReaderStore::libraryWorks, works -> {
            if (current != request) {
                return;
            }
            availability = new BackgroundTask<LibrarySnapshot>(cancelled -> {
                return LibraryInspector.inspect(works);
            }, task -> {
                if (current != request) {
                    return;
                }
                try {
                    panel.showSnapshot(task.get());
                } catch (CancellationException ignored) { } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    error("Cannot check library locations: " + exception.getCause().getMessage());
                }
            });
            availability.execute();
        }, message -> {
            if (current == request) {
                error(message);
            }
        });
    }

    void invalidateCovers() {
        coverGeneration.incrementAndGet();
        covers.clear();
    }

    void cover(LibraryChapter chapter, Consumer<BufferedImage> loaded) {
        covers.addLast(new LibraryCoverRequest(coverGeneration.get(), chapter, loaded));
        pumpCover();
    }

    private void pumpCover() {
        if (coverPending || covers.isEmpty()) {
            return;
        }
        LibraryCoverRequest next = covers.removeFirst();
        coverPending = true;
        session.libraryTask(store -> {
            if (next.getGeneration() != coverGeneration.get()) {
                return null;
            }
            BookState state = new BookState(next.getChapter().getSource().getPath(), "", 1, 1, false, ReadingMode.MANGA);
            RecentBook preview = RecentBookLoader.cover(RecentBookLoader.inspect(state));
            return preview.getCover();
        }, image -> {
            coverPending = false;
            if (next.getGeneration() == coverGeneration.get()) {
                next.getLoaded().accept(image);
            }
            pumpCover();
        }, ignored -> {
            coverPending = false;
            pumpCover();
        });
    }

    void open(LibraryChapter chapter) {
        // The normal reader validates the source and restores page, direction and spread mode.
        invalidateCovers();
        openBook.accept(new File(chapter.getSource().getPath()));
    }

    void rename(LibraryWork work) {
        String name = JOptionPane.showInputDialog(panel, "Library title (files will not be renamed):", work.getTitle());
        if (name != null && !name.isBlank()) {
            mutate(store -> store.renameLibrary(work.getId(), name));
        }
    }

    void remove(LibraryWork work) {
        if (JOptionPane.showConfirmDialog(panel, "Remove “" + work.getTitle() + "” from the library?\n"
                + "Files, recent history, reading progress and completion marks will be kept.",
                "Remove from library", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            mutate(store -> store.removeLibrary(work.getId()));
        }
    }

    void finish(LibraryChapter chapter) {
        mutate(store -> store.finishChapter(chapter.getId(), !chapter.isFinished()));
    }

    private void mutate(Consumer<ReaderStore> operation) {
        session.libraryTask(store -> {
            operation.accept(store);
            return true;
        }, ignored -> refresh(), this::error);
    }

    void chooseFolder() {
        if (importing) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Add a work to the library");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        importFolder(chooser.getSelectedFile().toPath());
    }

    public void importFolder(Path directory) {
        if (importing) {
            return;
        }
        File selected = directory.toFile();
        importing = true;
        JDialog progress = new JDialog(SwingUtilities.getWindowAncestor(panel), "Scanning folder", Dialog.ModalityType.APPLICATION_MODAL);
        JLabel label = new JLabel("  Checking ZIP chapters and image pages. Subfolders are not scanned.  ");
        JButton cancel = new JButton("Cancel");
        progress.add(label, BorderLayout.CENTER);
        progress.add(cancel, BorderLayout.SOUTH);
        progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        SwingWorker<LibraryImport, Void> scan = new BackgroundTask<LibraryImport>(cancelled -> {
            return new LibraryScanner().scan(selected.toPath(), cancelled);
        }, task -> {
            importing = false;
            progress.dispose();
            try {
                preview(task.get());
            } catch (CancellationException ignored) { } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException exception) {
                error("Cannot scan this folder: " + exception.getCause().getMessage());
            }
        });
        cancel.addActionListener(event -> scan.cancel(true));
        progress.pack();
        progress.setLocationRelativeTo(panel);
        scan.execute();
        progress.setVisible(true);
    }

    private void preview(LibraryImport found) {
        if (found.getSources().isEmpty()) {
            error("No supported books found. Subfolders are not scanned.\n" + String.join("\n", found.getWarnings()));
            return;
        }
        JTextField title = new JTextField(found.getTitle());
        JList<LibrarySource> chapters = new JList<>(found.getSources().toArray(LibrarySource[]::new));
        chapters.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Mixed folders require an explicit choice; homogeneous folders default to all chapters.
        boolean mixed = found.getSources().stream().anyMatch(LibrarySource::isDirectory)
                && found.getSources().stream().anyMatch(source -> !source.isDirectory());
        if (!mixed) {
            chapters.setSelectionInterval(0, found.getSources().size() - 1);
        }
        JPanel preview = new JPanel(new BorderLayout(8, 8));
        JPanel heading = new JPanel(new BorderLayout());
        heading.add(new JLabel("Title: "), BorderLayout.WEST);
        heading.add(title, BorderLayout.CENTER);
        preview.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(chapters);
        scroll.setPreferredSize(new Dimension(520, 300));
        preview.add(scroll, BorderLayout.CENTER);
        JTextArea explanation = new JTextArea((mixed ? "Mixed folder: select the image folder, ZIP chapters, or both.\n" : "Select chapters to add.\n")
                + "Files stay in place. Reimport adds chapters; existing progress is kept.\n"
                + String.join("\n", found.getWarnings()));
        explanation.setEditable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        JScrollPane warnings = new JScrollPane(explanation);
        warnings.setPreferredSize(new Dimension(520, 95));
        preview.add(warnings, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(panel, preview, "Import preview", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            List<LibrarySource> selected = chapters.getSelectedValuesList();
            if (title.getText().isBlank() || selected.isEmpty()) {
                error("Enter a title and select at least one source.");
                continue;
            }
            LibraryImport selection = new LibraryImport(found.getRoot(), title.getText(), selected, found.getWarnings());
            mutate(store -> store.importLibrary(selection));
            break;
        }
    }

    private void error(String message) {
        panel.status("Library operation failed; existing data was kept.");
        JOptionPane.showMessageDialog(panel, message, "Library", JOptionPane.ERROR_MESSAGE);
    }
}
