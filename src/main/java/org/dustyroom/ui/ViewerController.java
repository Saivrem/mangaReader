package org.dustyroom.ui;

import org.dustyroom.ui.components.ImageComponent;
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

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;

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
                DialogUtils.showError(frame, message);
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

        Action details = fileChooser.getActionMap().get("viewTypeDetails");
        if (details != null) {
            details.actionPerformed(null);
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

    public boolean setTheme(ViewerTheme theme) {
        if (!UiUtils.installTheme(theme)) {
            DialogUtils.showError(frame, "Unable to apply the selected theme.");
            return false;
        }
        SwingUtilities.updateComponentTreeUI(frame);
        configureViewerSurfaces();
        frame.revalidate();
        frame.repaint();
        return true;
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void exit() {
        session.close(() -> System.exit(0));
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

    public void configureViewerSurfaces() {
        Color background = UIManager.getColor("Viewer.background");
        if (background == null) {
            background = new Color(30, 33, 39);
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

}
