package org.dustyroom.ui;

import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.loading.ViewerView;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.ui.LookSettings.NIMBUS;
import static org.dustyroom.ui.LookSettings.SYSTEM;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;
import static org.dustyroom.ui.utils.UiUtils.redrawComponent;

public class ViewerController {
    private final JFrame frame;
    private final GraphicsDevice graphicsDevice;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final AsyncViewerSession session;

    private JComponent menuBar;
    private JComponent navigationPanel;
    private File currentDir;
    private boolean fullscreen;

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane) {
        this.frame = frame;
        this.graphicsDevice = graphicsDevice;
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

    public void attachPanels(JComponent menuBar, JComponent navigationPanel) {
        this.menuBar = menuBar;
        this.navigationPanel = navigationPanel;
    }

    public void openFile(File file) {
        session.open(file);
    }

    public void chooseFile() {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(null);
        }

        String root = currentDir == null ? System.getProperty("user.home") : currentDir.toString();
        JFileChooser fileChooser = new JFileChooser(root);
        fileChooser.setPreferredSize(new Dimension(800, 600));

        if (UiUtils.getCurrent() != SYSTEM) {
            Action details = fileChooser.getActionMap().get("viewTypeDetails");
            if (details != null) {
                details.actionPerformed(null);
            }
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS));
        fileChooser.setFileSelectionMode(FILES_ONLY);

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            openFile(fileChooser.getSelectedFile());
        }

        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
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

    public void setNimbusTheme() {
        UiUtils.setDarkTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
    }

    public void setMetalTheme() {
        UiUtils.setMetalTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
    }

    public void setSystemTheme() {
        UiUtils.setSystemTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void exit() {
        session.close(() -> System.exit(0));
    }

    public void close() {
        session.close();
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
        frame.requestFocusInWindow();
    }

    public void syncThemeSurfaces() {
        Color background = resolveThemeBackground();

        Container contentPane = frame.getContentPane();
        contentPane.setBackground(background);

        Border empty = BorderFactory.createEmptyBorder();
        scrollPane.setBorder(empty);
        scrollPane.setViewportBorder(empty);
        scrollPane.setOpaque(true);
        if (background == null) {
            background = new Color(34, 40, 49);
        }

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
        if (UiUtils.getCurrent() == NIMBUS) {
            Color nimbusBackground = UIManager.getColor("background");
            return nimbusBackground == null ? new Color(34, 40, 49) : nimbusBackground;
        }

        Color panelBackground = UIManager.getColor("Panel.background");
        return panelBackground == null ? UIManager.getColor("control") : panelBackground;
    }
}
