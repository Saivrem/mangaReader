package org.dustyroom.ui;

import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.persistence.ReaderSettings;
import org.dustyroom.persistence.ReaderStore;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.components.ReaderSurface;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.actions.ViewerActionState;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.loading.ViewerView;
import org.dustyroom.ui.utils.DialogUtils;
import org.dustyroom.ui.utils.UiUtils;
import org.dustyroom.ui.theme.ViewerTheme;
import javax.swing.SwingUtilities;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.io.File;

import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;

public class ViewerController {
    private final JFrame frame;
    private final GraphicsDevice graphicsDevice;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final AsyncViewerSession session;

    private JComponent menuBar;
    private NavigationPanel navigationPanel;
    private ReaderSurface readerSurface;
    private ViewerActions actions;
    private ViewerFrame displayedFrame;
    private org.dustyroom.ui.components.ThumbnailPanel thumbnailPanel;
    private boolean thumbnailsVisible;
    private String lastDirectory;
    private boolean choosingFile;
    private boolean fullscreen;
    private boolean resetting;

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane, ReaderStore store, ReaderSettings settings) {
        this.frame = frame;
        this.lastDirectory = settings.lastDirectory();
        this.thumbnailsVisible = settings.thumbnailsVisible();
        this.graphicsDevice = graphicsDevice;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
        this.session = new AsyncViewerSession(new ViewerView() {
            @Override
            public void showLoading() {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (navigationPanel != null) navigationPanel.setLoading(true);
                if (readerSurface != null) readerSurface.setLoading(true);
            }

            @Override public void showPages(java.util.List<org.dustyroom.be.models.PageRef> pages) {
                if (thumbnailPanel != null) thumbnailPanel.showPages(pages);
            }

            @Override
            public void showFrame(ViewerFrame viewerFrame) {
                applyFrame(viewerFrame);
            }

            @Override
            public void showPersistenceWarning(String message) {
                DialogUtils.showError(frame, message);
            }

            @Override
            public void showError(String message) {
                frame.setCursor(Cursor.getDefaultCursor());
                refreshControls();
                if (readerSurface != null) readerSurface.setLoading(false);
                DialogUtils.showError(frame, message);
            }
        }, store);
    }

    public void attachPanels(JComponent menuBar, NavigationPanel navigationPanel,
                             ReaderSurface readerSurface, ViewerActions actions) {
        this.menuBar = menuBar;
        this.navigationPanel = navigationPanel;
        this.readerSurface = readerSurface;
        this.actions = actions;
        refreshControls();
    }

    public org.dustyroom.ui.components.ThumbnailPanel createThumbnailPanel() {
        thumbnailPanel = new org.dustyroom.ui.components.ThumbnailPanel(session::thumbnail,
                session::goToPage, this::toggleThumbnails);
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(false);
        actions.toggleThumbnails().putValue(Action.SELECTED_KEY, thumbnailsVisible);
        return thumbnailPanel;
    }

    public void toggleThumbnails() {
        thumbnailsVisible = !thumbnailsVisible;
        actions.toggleThumbnails().putValue(Action.SELECTED_KEY, thumbnailsVisible);
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(displayedFrame != null);
        frame.revalidate();
        SwingUtilities.invokeLater(() -> { imageComponent.updateScale(); imageComponent.repaint(); });
        saveSettings();
    }

    public void openExternalFile(File file) {
        frame.setExtendedState(frame.getExtendedState() & ~java.awt.Frame.ICONIFIED);
        frame.toFront();
        frame.requestFocus();
        openFile(file);
    }

    public void openFile(File file) {
        session.open(file);
    }

    public void chooseFile() {
        if (choosingFile || resetting) return;
        choosingFile = true;
        actions.openFile().setEnabled(false);
        String requestedDirectory = lastDirectory;
        new javax.swing.SwingWorker<File, Void>() {
            @Override protected File doInBackground() {
                return org.dustyroom.ui.utils.OpenDirectory.resolve(requestedDirectory, System.getProperty("user.home"));
            }

            @Override protected void done() {
                try {
                    File root = get();
                    if (!resetting) showFileChooser(root);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException exception) {
                    DialogUtils.showError(frame, "Unable to access the last open directory.");
                } finally {
                    choosingFile = false;
                    actions.openFile().setEnabled(true);
                }
            }
        }.execute();
    }

    private void showFileChooser(File root) {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(null);
        }

        JFileChooser fileChooser = new JFileChooser(root);
        fileChooser.setPreferredSize(new Dimension(800, 600));

        Action details = fileChooser.getActionMap().get("viewTypeDetails");
        if (details != null) {
            details.actionPerformed(null);
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS));
        fileChooser.setFileSelectionMode(FILES_AND_DIRECTORIES);

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            openFile(fileChooser.getSelectedFile());
        }

        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        }
        frame.requestFocus();
    }

    public void choosePage() {
        if (displayedFrame == null) return;
        JSpinner page = new JSpinner(new SpinnerNumberModel(
                displayedFrame.firstPage(), 1, displayedFrame.pageCount(), 1));
        page.getAccessibleContext().setAccessibleName("Page number");
        while (JOptionPane.showConfirmDialog(frame, page,
                "Go to page (1–" + displayedFrame.pageCount() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                page.commitEdit();
                int number = ((Number) page.getValue()).intValue();
                if (number < 1 || number > displayedFrame.pageCount()) throw new java.text.ParseException("Out of range", 0);
                session.goToPage(number);
                return;
            } catch (java.text.ParseException exception) {
                DialogUtils.showError(frame, "Enter a page number between 1 and " + displayedFrame.pageCount() + ".");
            }
        }
    }

    public void showNextImage() {
        session.next();
    }

    public void showPreviousImage() {
        session.previous();
    }

    public void showFirstImage() {
        session.first();
    }

    public void showLastImage() {
        session.last();
    }

    public void showNextVolume() {
        session.nextVolume();
    }

    public void showPrevVolume() {
        session.previousVolume();
    }

    public void fitHeight() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_HEIGHT);
        saveSettings();
        refreshControls();
    }

    public void fitWidth() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_WIDTH);
        saveSettings();
        refreshControls();
    }

    public void fitScreen() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_SCREEN);
        saveSettings();
        refreshControls();
    }

    public void zoomIn() {
        imageComponent.zoomIn();
        saveSettings();
        refreshControls();
    }

    public void zoomOut() {
        imageComponent.zoomOut();
        saveSettings();
        refreshControls();
    }

    public void toggleTwoPageMode() {
        session.toggleTwoPageMode();
    }

    public void setComicsReadingMode() {
        session.setComicsReadingMode();
    }

    public void setMangaReadingMode() {
        session.setMangaReadingMode();
    }

    public boolean setTheme(ViewerTheme theme) {
        if (!UiUtils.installTheme(theme)) {
            DialogUtils.showError(frame, "Unable to apply the selected theme.");
            return false;
        }
        SwingUtilities.updateComponentTreeUI(frame);
        configureViewerSurfaces();
        frame.revalidate();
        frame.repaint();
        saveSettings();
        return true;
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void resetApplication() {
        if (resetting) return;
        Object[] options = {"Reset and quit", "Cancel"};
        int choice = JOptionPane.showOptionDialog(frame,
                "Reset all settings and reading history?\n\n"
                        + "Saved page positions and reading modes will be removed.\n"
                        + "Your image files, folders and ZIP archives will not be deleted.\n\n"
                        + "Yonaka Reader will close. The next launch will start with default settings.",
                "Reset Yonaka Reader", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (choice != 0) return;
        resetting = true;
        frame.setEnabled(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        session.resetAndClose(() -> System.exit(0), message -> {
            resetting = false;
            frame.setEnabled(true);
            frame.setCursor(Cursor.getDefaultCursor());
            refreshControls();
            if (readerSurface != null) readerSurface.setLoading(false);
            DialogUtils.showError(frame, message);
        });
    }

    public void exit() {
        if (resetting) return;
        session.close(() -> System.exit(0));
    }

    public void exitFullscreen() {
        if (fullscreen) toggleFullscreen();
    }

    public void toggleFullscreen() {
        if (fullscreen) {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(false);
            graphicsDevice.setFullScreenWindow(null);
            if (navigationPanel != null) navigationPanel.setVisible(true);
            if (menuBar != null) menuBar.setVisible(true);
            frame.setVisible(true);
        } else {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(true);
            graphicsDevice.setFullScreenWindow(frame);
            if (navigationPanel != null) navigationPanel.setVisible(false);
            if (menuBar != null) menuBar.setVisible(false);
            frame.setVisible(true);
        }

        fullscreen = !fullscreen;
        refreshControls();
        frame.requestFocusInWindow();
    }

    public void configureViewerSurfaces() {
        Color background = UIManager.getColor("Viewer.background");
        if (background == null) {
            background = UIManager.getColor("Panel.background");
        }

        Container contentPane = frame.getContentPane();
        contentPane.setBackground(background);

        Border empty = BorderFactory.createEmptyBorder();
        scrollPane.setBorder(empty);
        scrollPane.setViewportBorder(empty);
        scrollPane.setOpaque(true);
        imageComponent.setOpaque(true);
        imageComponent.setBackground(background);
        scrollPane.setBackground(background);

        JViewport viewport = scrollPane.getViewport();
        viewport.setOpaque(true);
        viewport.setBackground(background);
    }

    private void saveSettings() {
        double zoom = imageComponent.getFitMode() == ImageComponent.FitMode.ORIGINAL ? imageComponent.getScale() : 1.0;
        session.saveSettings(new ReaderSettings(UiUtils.currentTheme(), imageComponent.getFitMode(), zoom, lastDirectory, thumbnailsVisible));
    }

    private void refreshControls() {
        if (actions == null) return;
        ViewerActionState.apply(actions, displayedFrame, imageComponent.getFitMode(), fullscreen);
        navigationPanel.updateState(displayedFrame, imageComponent.getFitMode());
    }

    private void applyFrame(ViewerFrame viewerFrame) {
        frame.setCursor(Cursor.getDefaultCursor());
        displayedFrame = viewerFrame;
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(true);
        thumbnailPanel.showSpread(viewerFrame.firstPage(), viewerFrame.lastPage());
        readerSurface.showReading();
        String openedDirectory = viewerFrame.directory().toPath().toAbsolutePath().normalize().toString();
        if (!openedDirectory.equals(lastDirectory)) {
            lastDirectory = openedDirectory;
            saveSettings();
        }
        frame.setTitle(viewerFrame.title());
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        imageComponent.setImageAndCenter(viewerFrame.image(), scrollPane);
        session.remember(viewerFrame);
        refreshControls();
    }

}
