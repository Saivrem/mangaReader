package org.dustyroom.ui;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;
import org.dustyroom.ui.components.ImagePanel;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.components.listeners.ThemeChangeListener;
import org.dustyroom.ui.utils.UiUtils;

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
import static org.dustyroom.be.utils.FileUtils.isZipFile;
import static org.dustyroom.ui.LookSettings.SYSTEM;
import static org.dustyroom.ui.utils.DialogUtils.showAboutDialog;
import static org.dustyroom.ui.utils.UiUtils.redrawComponent;
import static org.dustyroom.ui.utils.UiUtils.setDarkTheme;

@Slf4j
public class ImageViewer extends JFrame {
    private final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private final GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
    private final MenuBar menuBar;
    private final NavigationPanel navigationPanel;
    private final ImagePanel imagePanel;
    private final JScrollPane scrollPane;
    @Setter
    private ImageIterator imageIterator;
    private File currentDir;
    private boolean fullscreen = false;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setDarkTheme();

        imagePanel = new ImagePanel();
        scrollPane = new JScrollPane(imagePanel);

        menuBar = new MenuBar(
                ImageViewer.this::chooseFile,
                ImageViewer.this.imagePanel::fitImageToHeight,
                ImageViewer.this.imagePanel::fitImageToWidth,
                ImageViewer.this.imagePanel::zoomIn,
                ImageViewer.this.imagePanel::zoomOut,
                ImageViewer.this::showNextImage,
                ImageViewer.this::showPreviousImage,
                ImageViewer.this::showFirstImage,
                ImageViewer.this::showLastImage,
                ImageViewer.this::showNextVolume,
                ImageViewer.this::showPrevVolume,
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
                ImageViewer.this::chooseFile,
                ImageViewer.this::showNextVolume,
                ImageViewer.this::showPrevVolume
        );

        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        add(menuBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (imagePanel.isFitToHeightEnabled()) {
                    imagePanel.fitImageToHeight();
                } else if (imagePanel.isFitToWidthEnabled()) {
                    imagePanel.fitImageToWidth();
                }
            }
        });

        setupControls();

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocus();
    }

    private void setupControls() {

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
                        imagePanel.fitImageToHeight();
                        break;
                    case KeyEvent.VK_W:
                        imagePanel.fitImageToWidth();
                        break;
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_EQUALS:
                        imagePanel.zoomIn();
                        break;
                    case KeyEvent.VK_MINUS:
                        imagePanel.zoomOut();
                        break;
                    case KeyEvent.VK_UP:
                        showPrevVolume();
                        break;
                    case KeyEvent.VK_DOWN:
                        showNextVolume();
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
        fileChooser.setPreferredSize(new Dimension(800, 600));

        // System LnF simply doesn't have this option
        if (UiUtils.getCurrent() != SYSTEM) {
            Action details = fileChooser.getActionMap().get("viewTypeDetails");
            details.actionPerformed(null);
        }

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(FILES_ONLY); //FILES_AND_DIRECTORIES

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                // Currently disabled due to FILES_ONLY selection mode
                imageIterator = new DirImageIterator();
            } else if (isZipFile.test(selectedFile)) {
                imageIterator = new ZipIterator(selectedFile);
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

    private void showNextVolume() {
        processPicture(imageIterator.nextVol());
    }

    private void showPrevVolume() {
        processPicture(imageIterator.prevVol());
    }

    public void processPicture(Picture picture) {
        PictureMetadata metadata = picture.metadata();
        currentDir = metadata.dir();
        setTitle(String.format("%s - %s", metadata.fileName(), metadata.name()));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
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
}
