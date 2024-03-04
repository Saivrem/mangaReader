package org.dustyroom.ui;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipImageIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.utils.UiUtils;
import org.dustyroom.ui.components.ImagePanel;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.components.listeners.ThemeChangeListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.UiUtils.redrawComponent;
import static org.dustyroom.ui.utils.DialogUtils.showAboutDialog;

@Slf4j
public class ImageViewer extends JFrame {
    private final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private final GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
    private final MenuBar menuBar;
    private final NavigationPanel navigationPanel;
    private final ImagePanel imagePanel;
    private ImageIterator imageIterator;
    private File currentDir;
    private boolean fullscreen = false;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));

        menuBar = new MenuBar(
                ImageViewer.this::chooseFile,
                ImageViewer.this::fitMode,
                ImageViewer.this::zoomIn,
                ImageViewer.this::zoomOut,
                ImageViewer.this::showNextImage,
                ImageViewer.this::showPreviousImage,
                ImageViewer.this::showFirstImage,
                ImageViewer.this::showLastImage,
                ImageViewer.this::toggleFullscreen,
                new ThemeChangeListener() {
                    @Override
                    public void setNimbusTheme() {
                        UiUtils.setDarkTheme();
                        redrawComponent(ImageViewer.this);
                    }

                    @Override
                    public void setMetalTheme() {
                        UiUtils.setMetalTheme();
                        redrawComponent(ImageViewer.this);
                    }

                    @Override
                    public void setSystemTheme() {
                        UiUtils.setSystemTheme();
                        redrawComponent(ImageViewer.this);
                    }
                },
                () -> showAboutDialog(ImageViewer.this)
        );

        navigationPanel = new NavigationPanel(
                ImageViewer.this::showNextImage,
                ImageViewer.this::showPreviousImage,
                ImageViewer.this::showFirstImage,
                ImageViewer.this::showLastImage,
                ImageViewer.this::chooseFile
        );

        imagePanel = new ImagePanel();

        initializeUI();

        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        add(menuBar, BorderLayout.NORTH);
        add(imagePanel, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagePanel.repaint();
            }
        });

        setupControls();

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocus();
    }

    private void setupControls() {

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (notches < 0) {
                showPreviousImage();
            } else {
                showNextImage();
            }
            imagePanel.repaint();
        });
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // NOOP
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Extract file walking logic into separate class
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_PAGE_DOWN:
                        showNextImage();
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_PAGE_UP:
                        showPreviousImage();
                        break;
                    case KeyEvent.VK_HOME:
                        showFirstImage();
                        break;
                    case KeyEvent.VK_END:
                        showLastImage();
                        break;
                    case KeyEvent.VK_F:
                        toggleFullscreen();
                        break;
                    case KeyEvent.VK_O:
                        chooseFile();
                        break;
                    case KeyEvent.VK_H:
                        fitMode();
                        break;
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_EQUALS:
                        zoomIn();
                        break;
                    case KeyEvent.VK_MINUS:
                        zoomOut();
                        break;
                    case KeyEvent.VK_ESCAPE:
                    case KeyEvent.VK_Q:
                        System.exit(0);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // NOOP
            }
        });
    }

    private void chooseFile() {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(null);
        }
        String root = currentDir == null ? System.getProperty("user.home") : currentDir.toString();
        JFileChooser fileChooser = new JFileChooser(root);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(FILES_ONLY); //FILES_AND_DIRECTORIES

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                imageIterator = new DirImageIterator();
            } else if (selectedFile.toString().endsWith(".zip")) {
                imageIterator = new ZipImageIterator(selectedFile);
            } else {
                imageIterator = new FileImageIterator(selectedFile);
            }
            processPicture(imageIterator.next());
        }
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(this);
        }
        requestFocus();
    }

    private void showNextImage() {
        processPicture(imageIterator.next());
    }

    private void showPreviousImage() {
        processPicture(imageIterator.prev());
    }

    private void showFirstImage() {
        processPicture(imageIterator.first());
    }

    private void showLastImage() {
        processPicture(imageIterator.last());
    }

    private void processPicture(Picture picture) {
        currentDir = picture.metadata().dir();
        setTitle(picture.metadata().name());
        imagePanel.drawImage(picture.image());
    }

    private void toggleFullscreen() {
        if (fullscreen) {
            setVisible(false);
            dispose();
            setUndecorated(false);
            graphicsDevice.setFullScreenWindow(null);
            navigationPanel.setVisible(true);
            menuBar.setVisible(true);
            setVisible(true);
        } else {
            setVisible(false);
            dispose();
            setUndecorated(true);
            graphicsDevice.setFullScreenWindow(this);
            navigationPanel.setVisible(false);
            menuBar.setVisible(false);
            setVisible(true);
        }

        fullscreen = !fullscreen;
        requestFocusInWindow();
    }

    private void fitMode() {
        imagePanel.toggleZoomMode();
        imagePanel.repaint();
    }

    private void zoomOut() {
        if (imagePanel.isFitMode()) {
            imagePanel.toggleZoomMode();
        }
        imagePanel.zoomOut();
        imagePanel.repaint();
    }

    private void zoomIn() {
        if (imagePanel.isFitMode()) {
            imagePanel.toggleZoomMode();
        }
        imagePanel.zoomIn();
        imagePanel.repaint();
    }
}
