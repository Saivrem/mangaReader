package org.dustyroom.ui;

import lombok.Setter;
import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.FileUtils.isZipFile;
import static org.dustyroom.ui.LookSettings.SYSTEM;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;
import static org.dustyroom.ui.utils.UiUtils.redrawComponent;

public class ViewerController {
    private final JFrame frame;
    private final GraphicsDevice graphicsDevice;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;

    @Setter
    private ImageIterator imageIterator;

    private JComponent menuBar;
    private JComponent navigationPanel;
    private File currentDir;
    private boolean fullscreen;

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane) {
        this.frame = frame;
        this.graphicsDevice = graphicsDevice;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
    }

    public void attachPanels(JComponent menuBar, JComponent navigationPanel) {
        this.menuBar = menuBar;
        this.navigationPanel = navigationPanel;
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

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(FILES_ONLY);

        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                imageIterator = new DirImageIterator();
            } else if (isZipFile.test(selectedFile)) {
                imageIterator = new ZipIterator(selectedFile);
            } else {
                imageIterator = new FileImageIterator(selectedFile);
            }
            processPicture(imageIterator.next());
        }
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        }

        frame.requestFocus();
    }

    public void showNextImage() {
        if (imageIterator != null) {
            processPicture(imageIterator.next());
        }
    }

    public void showPreviousImage() {
        if (imageIterator != null) {
            processPicture(imageIterator.prev());
        }
    }

    public void showFirstImage() {
        if (imageIterator != null) {
            processPicture(imageIterator.first());
        }
    }

    public void showLastImage() {
        if (imageIterator != null) {
            processPicture(imageIterator.last());
        }
    }

    public void showNextVolume() {
        if (imageIterator != null) {
            processPicture(imageIterator.nextVol());
        }
    }

    public void showPrevVolume() {
        if (imageIterator != null) {
            processPicture(imageIterator.prevVol());
        }
    }

    public void processPicture(Picture picture) {
        if (picture == null) {
            return;
        }

        PictureMetadata metadata = picture.metadata();
        currentDir = metadata.dir();
        frame.setTitle(String.format("%s - %s", metadata.fileName(), metadata.name()));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        imageComponent.setImageAndCenter(picture.image(), scrollPane);
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

    public void setNimbusTheme() {
        UiUtils.setDarkTheme();
        redrawComponent(frame);
    }

    public void setMetalTheme() {
        UiUtils.setMetalTheme();
        redrawComponent(frame);
    }

    public void setSystemTheme() {
        UiUtils.setSystemTheme();
        redrawComponent(frame);
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void exit() {
        System.exit(0);
    }

    public void toggleFullscreen() {
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
        frame.requestFocusInWindow();
    }
}
