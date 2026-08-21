package org.dustyroom.ui;

import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.actions.ViewerCommandPort;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.loading.ViewerView;
import org.dustyroom.ui.theme.ThemeManager;
import org.dustyroom.ui.window.WindowStateController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Objects;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.ui.LookSettings.MANGA_READER_DARK;
import static org.dustyroom.ui.LookSettings.MANGA_READER_LIGHT;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;

public class ViewerController implements ViewerCommandPort {
    private final JFrame frame;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final AsyncViewerSession session;

    private WindowStateController windowStateController;
    private File currentDir;

    public ViewerController(JFrame frame, ImageComponent imageComponent, JScrollPane scrollPane) {
        this.frame = frame;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
        this.session = new AsyncViewerSession(new ViewerView() {
            @Override
            public void showLoading() {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }

            @Override
            public void showFrame(ViewerFrame viewerFrame) {
                applyFrame(viewerFrame);
            }

            @Override
            public void showError(String message) {
                frame.setCursor(Cursor.getDefaultCursor());
                org.dustyroom.ui.utils.DialogUtils.showError(frame, message);
            }
        });
    }

    public void attachWindowStateController(WindowStateController windowStateController) {
        if (this.windowStateController != null) {
            throw new IllegalStateException("Window state controller is already attached");
        }
        this.windowStateController = Objects.requireNonNull(windowStateController);
    }

    public void openFile(File file) {
        session.open(file);
    }

    public void chooseFile() {
        String root = currentDir == null ? System.getProperty("user.home") : currentDir.toString();
        JFileChooser fileChooser = new JFileChooser(root);
        fileChooser.setPreferredSize(new Dimension(800, 600));
        fileChooser.setAcceptAllFileFilterUsed(false);

        Action details = fileChooser.getActionMap().get("viewTypeDetails");
        if (details != null) {
            details.actionPerformed(null);
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS));
        fileChooser.setFileSelectionMode(FILES_ONLY);

        int result = windowState().withFullscreenSuspended(() -> fileChooser.showOpenDialog(frame));
        if (result == JFileChooser.APPROVE_OPTION) {
            openFile(fileChooser.getSelectedFile());
        }

        frame.requestFocus();
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
    }

    public void fitWidth() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_WIDTH);
    }

    public void fitScreen() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_SCREEN);
    }

    public void zoomIn() {
        imageComponent.zoomIn();
    }

    public void zoomOut() {
        imageComponent.zoomOut();
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

    @Override
    public boolean setDarkTheme() {
        return applyTheme(MANGA_READER_DARK);
    }

    @Override
    public boolean setLightTheme() {
        return applyTheme(MANGA_READER_LIGHT);
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void exit() {
        session.close(() -> {
            if (windowStateController != null) {
                windowStateController.close();
            }
            frame.dispose();
            System.exit(0);
        });
    }

    public void close() {
        session.close(() -> {
            if (windowStateController != null) {
                windowStateController.close();
            }
        });
    }

    public void toggleFullscreen() {
        windowState().toggleFullscreen();
        frame.requestFocusInWindow();
    }

    public void escapeFullscreen() {
        if (windowStateController != null && windowStateController.isFullscreen()) {
            windowStateController.exitFullscreen();
            frame.requestFocusInWindow();
        }
    }

    public void syncThemeSurfaces() {
        Color background = resolveThemeBackground();
        if (background == null) {
            background = new Color(34, 40, 49);
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

    private void applyFrame(ViewerFrame viewerFrame) {
        frame.setCursor(Cursor.getDefaultCursor());
        currentDir = viewerFrame.directory();
        frame.setTitle(viewerFrame.title());
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        imageComponent.setImageAndCenter(viewerFrame.image(), scrollPane);
    }

    private Color resolveThemeBackground() {
        Color applicationCanvas = UIManager.getColor("MangaReader.canvasBackground");
        if (applicationCanvas != null) {
            return applicationCanvas;
        }
        Color panelBackground = UIManager.getColor("Panel.background");
        return panelBackground == null ? UIManager.getColor("control") : panelBackground;
    }

    private boolean applyTheme(LookSettings theme) {
        if (!ThemeManager.applyToOpenWindows(theme)) {
            return false;
        }
        syncThemeSurfaces();
        return true;
    }

    private WindowStateController windowState() {
        if (windowStateController == null) {
            throw new IllegalStateException("Window state controller is not attached");
        }
        return windowStateController;
    }
}
