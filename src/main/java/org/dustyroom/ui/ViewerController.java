package org.dustyroom.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.state.ReaderSettings;
import org.dustyroom.be.models.state.ThemeMode;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.be.persistence.ReaderStore;
import org.dustyroom.be.settings.ProfileManager;
import org.dustyroom.be.utils.OpenDirectory;
import org.dustyroom.be.utils.StorageUtils;
import org.dustyroom.ui.actions.ViewerActionState;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.components.ReaderGestures;
import org.dustyroom.ui.components.ReaderSurface;
import org.dustyroom.ui.components.ThumbnailPanel;
import org.dustyroom.ui.library.LibraryController;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.BackgroundTask;
import org.dustyroom.ui.loading.ViewerView;
import org.dustyroom.ui.settings.SettingsDialog;
import org.dustyroom.ui.settings.SettingsHost;
import org.dustyroom.ui.settings.SystemThemeMonitor;
import org.dustyroom.ui.utils.DialogUtils;
import org.dustyroom.ui.utils.UiUtils;

import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;

public class ViewerController implements SettingsHost, ViewerView {
    private ReaderSettings preferences;
    private final Path database;
    private final SystemThemeMonitor themeMonitor;
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
    private ThumbnailPanel thumbnailPanel;
    private LibraryController libraryController;
    private boolean thumbnailsVisible;
    private boolean groupRecentByFolder;
    private String lastDirectory;
    private boolean choosingFile;
    private boolean fullscreen;
    private boolean resetting;
    private boolean profileSwitchPending;
    private boolean applyingSettings;

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane, ReaderStore store, ReaderSettings settings) {
        this.frame = frame;
        this.preferences = settings.toBuilder().build();
        this.database = store.databasePath();
        this.lastDirectory = settings.getLastDirectory();
        this.thumbnailsVisible = settings.isThumbnailsVisible();
        this.groupRecentByFolder = settings.isGroupRecentByFolder();
        this.graphicsDevice = graphicsDevice;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
        this.session = new AsyncViewerSession(this, store);
        session.configureDefaults(preferences);
        themeMonitor = new SystemThemeMonitor(preferences.getThemeMode(), theme -> {
            if (theme != UiUtils.currentTheme()) {
                applyTheme(theme);
                saveSettings();
            }
        });
    }

    public void attachPanels(JComponent menuBar, NavigationPanel navigationPanel,
                             ReaderSurface readerSurface, ViewerActions actions) {
        this.menuBar = menuBar;
        this.navigationPanel = navigationPanel;
        this.readerSurface = readerSurface;
        this.actions = actions;
        libraryController = new LibraryController(session, this::openFile);
        readerSurface.setLibrary(libraryController.panel(), libraryController::refresh);
        ReaderGestures.install(imageComponent, scrollPane, () -> {
            saveSettings();
            refreshControls();
        });
        Consumer<String> openRecent = id -> openFile(new File(id));
        readerSurface.setRecentHandlers(openRecent, session::hideRecentBook);
        if (menuBar instanceof MenuBar menu) {
            menu.setRecentHandlers(openRecent, session::refreshRecentBooks);
        }
        readerSurface.setRecentGrouping(groupRecentByFolder, grouped -> {
            groupRecentByFolder = grouped;
            session.setGroupRecentByFolder(grouped);
            saveSettings();
        });
        session.setGroupRecentByFolder(groupRecentByFolder);
        refreshControls();
    }

    public ThumbnailPanel createThumbnailPanel() {
        thumbnailPanel = new ThumbnailPanel(session::thumbnail,
                page -> {
            session.goToPage(page);
                    // Leave the list's local navigation bindings after mouse or Enter activation.
            frame.requestFocusInWindow();
        }, this::toggleThumbnails);
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(false);
        actions.toggleThumbnails().putValue(Action.SELECTED_KEY, thumbnailsVisible);
        return thumbnailPanel;
    }

    public void openPageGrid() {
        if (thumbnailPanel == null || !thumbnailPanel.isVisible()) {
            return;
        }
        if (!thumbnailsVisible) {
            toggleThumbnails();
        }
        thumbnailPanel.setGridMode(true);
    }

    public void toggleThumbnails() {
        thumbnailsVisible = !thumbnailsVisible;
        actions.toggleThumbnails().putValue(Action.SELECTED_KEY, thumbnailsVisible);
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(displayedFrame != null);
        frame.revalidate();
        SwingUtilities.invokeLater(() -> {
            imageComponent.updateScale();
            imageComponent.repaint();
        });
        saveSettings();
    }

    public void showHome() {
        if (resetting) {
            return;
        }
        thumbnailPanel.setGridMode(false);
        if (fullscreen) {
            exitFullscreen();
        }
        readerSurface.showWelcome();
        libraryController.refresh();
        if (thumbnailPanel != null) {
            thumbnailPanel.setVisible(false);
            actions.openPageGrid().setEnabled(false);
        }
        frame.setTitle("Yonaka Reader");
        session.refreshRecentBooks();
    }

    public void openExternalFile(File file) {
        frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
        frame.toFront();
        frame.requestFocus();
        openFile(file);
    }

    public void openFile(File file) {
        imageComponent.setAnimationPaused(true);
        session.open(file);
    }

    public void chooseFile() {
        if (choosingFile || resetting) {
            return;
        }
        choosingFile = true;
        imageComponent.setAnimationPaused(true);
        actions.openFile().setEnabled(false);
        String requestedDirectory = lastDirectory;
        new BackgroundTask<File>(cancelled -> {
            return OpenDirectory.resolve(requestedDirectory, System.getProperty("user.home"));
        }, task -> {
            try {
                File root = task.get();
                if (!resetting) {
                    showFileChooser(root);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                imageComponent.setAnimationPaused(false);
            } catch (ExecutionException exception) {
                imageComponent.setAnimationPaused(false);
                DialogUtils.showError(frame, "Unable to access the last open directory.");
            } finally {
                choosingFile = false;
                actions.openFile().setEnabled(true);
            }
        }).execute();
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
        } else {
            imageComponent.setAnimationPaused(false);
        }

        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        }
        frame.requestFocus();
    }

    public void choosePage() {
        if (displayedFrame == null) {
            return;
        }
        JSpinner page = new JSpinner(new SpinnerNumberModel(
                displayedFrame.getFirstPage(), 1, displayedFrame.getPageCount(), 1));
        page.getAccessibleContext().setAccessibleName("Page number");
        while (JOptionPane.showConfirmDialog(frame, page,
                "Go to page (1–" + displayedFrame.getPageCount() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                page.commitEdit();
                int number = ((Number) page.getValue()).intValue();
                if (number < 1 || number > displayedFrame.getPageCount()) {
                    throw new ParseException("Out of range", 0);
                }
                session.goToPage(number);
                return;
            } catch (ParseException exception) {
                DialogUtils.showError(frame, "Enter a page number between 1 and " + displayedFrame.getPageCount() + ".");
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
        imageComponent.setFitMode(FitMode.FIT_HEIGHT);
        saveSettings();
        refreshControls();
    }

    public void fitWidth() {
        imageComponent.setFitMode(FitMode.FIT_WIDTH);
        saveSettings();
        refreshControls();
    }

    public void fitScreen() {
        imageComponent.setFitMode(FitMode.FIT_SCREEN);
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
        if (!applyTheme(theme)) {
            return false;
        }
        preferences.setThemeMode(theme == ViewerTheme.LIGHT ? ThemeMode.LIGHT : ThemeMode.DARK);
        themeMonitor.setMode(preferences.getThemeMode());
        saveSettings();
        return true;
    }

    private boolean applyTheme(ViewerTheme theme) {
        if (!UiUtils.installTheme(theme)) {
            DialogUtils.showError(frame, "Unable to apply the selected theme.");
            return false;
        }
        SwingUtilities.updateComponentTreeUI(frame);
        for (Window window : frame.getOwnedWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        configureViewerSurfaces();
        frame.revalidate();
        frame.repaint();
        preferences.setTheme(theme);
        if (menuBar instanceof MenuBar menu) {
            menu.showTheme(theme);
        }
        return true;
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void resetApplication() {
        if (resetting || profileSwitchPending) {
            return;
        }
        Object[] options = {"Reset and quit", "Cancel"};
        int choice = JOptionPane.showOptionDialog(frame,
                "Reset all settings, library and reading history?\n\n"
                        + "Saved page positions and reading modes will be removed.\n"
                        + "Your image files, folders and ZIP archives will not be deleted.\n\n"
                        + "Yonaka Reader will close. The next launch will start with default settings.",
                "Reset Yonaka Reader", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (choice != 0) {
            return;
        }
        resetting = true;
        frame.setEnabled(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        session.resetAndClose(() -> {
            themeMonitor.close();
            System.exit(0);
        }, message -> {
            resetting = false;
            frame.setEnabled(true);
            frame.setCursor(Cursor.getDefaultCursor());
            refreshControls();
            if (readerSurface != null) {
                readerSurface.setLoading(false);
            }
            DialogUtils.showError(frame, message);
        });
    }

    public void exit() {
        if (resetting || profileSwitchPending) {
            return;
        }
        themeMonitor.close();
        session.close(() -> System.exit(0));
    }

    public void exitFullscreen() {
        if (thumbnailPanel != null && thumbnailPanel.isGridMode()) {
            thumbnailPanel.setGridMode(false);
            return;
        }
        if (fullscreen) {
            toggleFullscreen();
        }
    }

    public void toggleFullscreen() {
        if (thumbnailPanel != null) {
            thumbnailPanel.setGridMode(false);
        }
        if (fullscreen) {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(false);
            graphicsDevice.setFullScreenWindow(null);
            if (navigationPanel != null) {
                navigationPanel.setVisible(true);
            }
            if (menuBar != null) {
                menuBar.setVisible(true);
            }
            frame.setVisible(true);
        } else {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(true);
            graphicsDevice.setFullScreenWindow(frame);
            if (navigationPanel != null) {
                navigationPanel.setVisible(false);
            }
            if (menuBar != null) {
                menuBar.setVisible(false);
            }
            frame.setVisible(true);
        }

        fullscreen = !fullscreen;
        thumbnailPanel.setFullscreen(fullscreen);
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
        if (!applyingSettings) {
            session.saveSettings(currentSettings());
        }
    }

    @Override
    public ReaderSettings currentSettings() {
        double zoom = imageComponent.getFitMode() == FitMode.ORIGINAL ? imageComponent.getScale() : 1.0;
        return preferences.toBuilder().theme(UiUtils.currentTheme()).fitMode(imageComponent.getFitMode()).zoom(zoom)
                .lastDirectory(lastDirectory).thumbnailsVisible(thumbnailsVisible).groupRecentByFolder(groupRecentByFolder).build();
    }

    public void showSettings() {
        if (!resetting) {
            new SettingsDialog(frame, this).setVisible(true);
        }
    }

    @Override
    public void applySettings(ReaderSettings selected, Runnable success, Consumer<String> failure) {
        if (database == null) {
            failure.accept("Persistent storage is unavailable. Settings have not been saved.");
            return;
        }
        ViewerTheme theme = switch (selected.getThemeMode()) {
            case DARK -> ViewerTheme.DARK;
            case LIGHT -> ViewerTheme.LIGHT;
            case SYSTEM -> UiUtils.currentTheme();
        };
        ReaderSettings saved = selected.toBuilder().theme(theme).build();
        applyingSettings = true;
        session.saveSettings(saved, () -> {
            applyingSettings = false;
            preferences = saved;
            thumbnailsVisible = saved.isThumbnailsVisible();
            thumbnailPanel.setExpanded(thumbnailsVisible);
            actions.toggleThumbnails().putValue(Action.SELECTED_KEY, thumbnailsVisible);
            session.configureDefaults(saved);
            themeMonitor.setMode(saved.getThemeMode());
            applyTheme(theme);
            success.run();
        }, message -> {
            applyingSettings = false;
            failure.accept(message);
        });
    }

    @Override
    public Path dataDirectory() {
        return database == null ? null : database.getParent();
    }

    @Override
    public void scanLibrary(Path directory) {
        libraryController.importFolder(directory);
    }

    @Override
    public void switchDataDirectory(Path target, boolean existing, Runnable accepted) {
        if (resetting || profileSwitchPending || database == null || StorageUtils.isDirectoryOverridden()) {
            return;
        }
        profileSwitchPending = true;
        CompletableFuture.runAsync(() -> {
            try {
                ProfileManager.validateTarget(database, target, existing);
            } catch (IOException exception) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
        }).whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (error != null) {
                profileSwitchPending = false;
                DialogUtils.showError(frame, "Cannot select this data folder: " + error.getCause().getMessage());
                return;
            }
            accepted.run();
            resetting = true;
            frame.setEnabled(false);
            themeMonitor.close();
            session.close(() -> CompletableFuture.runAsync(() -> {
                try {
                    ProfileManager.switchProfile(StorageUtils.standardDirectory(), database, target, existing);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception.getMessage(), exception);
                }
            }).whenComplete((result, failure) -> SwingUtilities.invokeLater(() -> {
                if (failure != null) {
                    DialogUtils.showError(frame, "Profile switch failed. Restart the reader to reopen the previous profile.\n" + failure.getCause().getMessage());
                }
                System.exit(failure == null ? 0 : 1);
            })));
        }));
    }

    private void refreshControls() {
        if (actions == null) {
            return;
        }
        ViewerActionState.apply(actions, displayedFrame, imageComponent.getFitMode(), fullscreen);
        actions.openPageGrid().setEnabled(displayedFrame != null && thumbnailPanel != null && thumbnailPanel.isVisible());
        navigationPanel.updateState(displayedFrame, imageComponent.getFitMode());
    }

    private void applyFrame(ViewerFrame viewerFrame) {
        frame.setCursor(Cursor.getDefaultCursor());
        displayedFrame = viewerFrame;
        thumbnailPanel.setExpanded(thumbnailsVisible);
        thumbnailPanel.setVisible(true);
        thumbnailPanel.showSpread(viewerFrame.getFirstPage(), viewerFrame.getLastPage());
        readerSurface.showReading();
        String openedDirectory = viewerFrame.getDirectory().toPath().toAbsolutePath().normalize().toString();
        if (!openedDirectory.equals(lastDirectory)) {
            lastDirectory = openedDirectory;
            saveSettings();
        }
        frame.setTitle(viewerFrame.getTitle());
        frame.validate();
        imageComponent.setImageAndCenter(viewerFrame.getImage(), scrollPane);
        session.remember(viewerFrame);
        refreshControls();
    }

    @Override
    public void showAnimations(PageKey page, List<AnimatedPage> animations) {
        if (displayedFrame != null && page.equals(displayedFrame.getPageKey())) {
            imageComponent.setAnimations(animations);
        }
    }

    @Override
    public void useDefaultScale(FitMode mode) {
        imageComponent.restoreView(mode, 1.0);
    }

    @Override
    public void showLoading() {
        if (thumbnailPanel != null) {
            thumbnailPanel.setGridMode(false);
        }
        imageComponent.setAnimationPaused(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (navigationPanel != null) {
            navigationPanel.setLoading(true);
        }
        if (readerSurface != null) {
            readerSurface.setLoading(true);
        }
    }

    @Override
    public void showRecentBooks(List<RecentBook> books) {
        if (readerSurface != null) {
            readerSurface.showRecentBooks(books);
        }
        if (menuBar instanceof MenuBar menu) {
            menu.showRecentBooks(books);
        }
    }

    @Override
    public void showPages(List<PageRef> pages) {
        if (thumbnailPanel != null) {
            thumbnailPanel.showPages(pages);
        }
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
        imageComponent.setAnimationPaused(false);
        frame.setCursor(Cursor.getDefaultCursor());
        refreshControls();
        if (readerSurface != null) {
            readerSurface.setLoading(false);
        }
        DialogUtils.showError(frame, message);
    }

}
