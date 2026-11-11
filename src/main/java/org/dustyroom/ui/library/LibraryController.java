package org.dustyroom.ui.library;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.dustyroom.be.library.LibraryInspector;
import org.dustyroom.be.library.LibraryScanner;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.library.*;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.BackgroundTask;
import org.dustyroom.ui.loading.RecentBookLoader;

import javax.swing.*;
import java.awt.*;
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

/**
 * UI orchestration only. Import scanning and persistence never run on the EDT.
 */
public final class LibraryController {
    private final AsyncViewerSession session;
    private final Consumer<File> openBook;
    private final AtomicLong coverGeneration = new AtomicLong();
    @Getter
    @Accessors(fluent = true)
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
            availability = new BackgroundTask<>(_ -> LibraryInspector.inspect(works), task -> {
                if (current != request) {
                    return;
                }
                try {
                    panel.showSnapshot(task.get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException exception) {
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
        session.libraryTask(_ -> {
            if (next.generation() != coverGeneration.get()) {
                return null;
            }
            BookState state = new BookState(next.chapter().getSource().getPath(), "", 1, 1, false, ReadingMode.MANGA);
            RecentBook preview = RecentBookLoader.cover(RecentBookLoader.inspect(state));
            return preview.getCover();
        }, image -> {
            coverPending = false;
            if (next.generation() == coverGeneration.get()) {
                next.loaded().accept(image);
            }
            pumpCover();
        }, _ -> {
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
        String message = """
                Remove “%s” from the library?
                Files, recent history, reading progress and completion marks will be kept.""".formatted(work.getTitle());
        if (JOptionPane.showConfirmDialog(panel, message,
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
        }, _ -> refresh(), this::error);
    }

    void chooseFolder() {
        if (importing) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Add books from a folder");
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
        JLabel label = new JLabel("  Checking books in the folder and its immediate subfolders…  ");
        JButton cancel = new JButton("Cancel");
        progress.add(label, BorderLayout.CENTER);
        progress.add(cancel, BorderLayout.SOUTH);
        progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        SwingWorker<LibraryScan, Void> scan = new BackgroundTask<>(
                cancelled -> new LibraryScanner().scan(selected.toPath(), cancelled),
                task -> {
                    importing = false;
                    progress.dispose();
                    try {
                        preview(task.get());
                    } catch (CancellationException ignored) {
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException exception) {
                        error("Cannot scan this folder: " + exception.getCause().getMessage());
                    }
                });
        cancel.addActionListener(_ -> scan.cancel(true));
        progress.pack();
        progress.setLocationRelativeTo(panel);
        scan.execute();
        progress.setVisible(true);
    }

    private void preview(LibraryScan found) {
        String warningsText = String.join("\n", found.warnings());
        if (found.works().isEmpty()) {
            error("""
                    No supported books found in the folder or its immediate subfolders.
                    %s""".formatted(warningsText));
            return;
        }
        LibraryImportTableModel model = new LibraryImportTableModel(found.works());
        JTable chapters = new JTable(model);
        chapters.getColumnModel().getColumn(0).setMaxWidth(65);
        chapters.putClientProperty("terminateEditOnFocusLost", true);
        JPanel preview = new JPanel(new BorderLayout(8, 8));
        JScrollPane scroll = new JScrollPane(chapters);
        scroll.setPreferredSize(new Dimension(580, 300));
        preview.add(scroll, BorderLayout.CENTER);
        JTextArea explanation = new JTextArea("""
                Select chapters to import. Existing chapters keep their library book and progress.
                %s""".formatted(warningsText));
        explanation.setEditable(false);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);
        JScrollPane warnings = new JScrollPane(explanation);
        warnings.setPreferredSize(new Dimension(580, 85));
        preview.add(warnings, BorderLayout.SOUTH);
        while (JOptionPane.showConfirmDialog(panel, preview, "Import preview", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            if (chapters.isEditing()) {
                chapters.getCellEditor().stopCellEditing();
            }
            List<LibraryImport> selected = model.selection();
            boolean missingTitle = selected
                    .stream()
                    .anyMatch(work -> work.getTitle().isBlank());
            if (selected.isEmpty() || missingTitle) {
                error("Select at least one chapter and enter a title for each selected book.");
                continue;
            }
            mutate(store -> store.importWorks(selected));
            return;
        }
    }

    private void error(String message) {
        panel.status("Library operation failed; existing data was kept.");
        JOptionPane.showMessageDialog(panel, message, "Library", JOptionPane.ERROR_MESSAGE);
    }
}
