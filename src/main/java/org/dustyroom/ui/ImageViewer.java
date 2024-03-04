package org.dustyroom.ui;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipImageIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.ui.panels.ImagePanel;

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
import static org.dustyroom.be.utils.UiUtils.*;
import static org.dustyroom.ui.utils.DialogUtils.showAboutDialog;

@Slf4j
public class ImageViewer extends JFrame {

    private final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private final GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();

    // Menu
    private final JMenuBar menuBar = new JMenuBar();

    // Buttons
    private final JButton openButton = new JButton("\uD83D\uDCC2");
    private final JButton nextButton = new JButton("→");
    private final JButton prevButton = new JButton("←");
    private final JButton firstButton = new JButton("⇤");
    private final JButton lastButton = new JButton("⇥");
    // Panels
    private final JPanel southPanel = new JPanel(new FlowLayout());
    private final ImagePanel imagePanel = new ImagePanel();
    // Other
    private ImageIterator imageIterator;
    private boolean fullscreen = false;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        // this.fileTreeIterator = fileTreeIterator;
        initializeUI();

        setVisible(true);
    }

    private void initializeUI() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagePanel.repaint();
            }
        });

        setLayout(new BorderLayout());

        setupMenuBar();
        setupButtons();
        setupControls();

        add(menuBar, BorderLayout.NORTH);
        add(imagePanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocus();
    }

    private void setupButtons() {
        southPanel.add(firstButton);
        southPanel.add(prevButton);
        southPanel.add(openButton);
        southPanel.add(nextButton);
        southPanel.add(lastButton);

        firstButton.addActionListener(e -> showFirstImage());
        prevButton.addActionListener(e -> showPreviousImage());
        openButton.addActionListener(e -> chooseFile());
        nextButton.addActionListener(e -> showNextImage());
        lastButton.addActionListener(e -> showLastImage());
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

    private void setupMenuBar() {
        JMenu fileMenu = buildFileMenu();
        JMenu viewMenu = buildViewMenu();
        JMenu navigationMenu = buildNavigationMenu();
        JMenu optionsMenu = buildOptionsMenu();
        JMenu helpMenu = buildHelpMenu();

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(navigationMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open (O)");
        JMenuItem exitMenuItem = new JMenuItem("Exit (Q)");

        openMenuItem.addActionListener(e -> chooseFile());
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private JMenu buildViewMenu() {
        JMenu viewMenu = new JMenu("View");
        JMenuItem fitMenuItem = new JMenuItem("Fit height mode (h)");
        JMenuItem zoomInMenuItem = new JMenuItem("Zoom In (+)");
        JMenuItem zoomOutMenuItem = new JMenuItem("Zoom out (-)");

        fitMenuItem.addActionListener(e -> fitMode());

        zoomInMenuItem.addActionListener(e -> zoomIn());
        zoomOutMenuItem.addActionListener(e -> zoomOut());

        viewMenu.add(fitMenuItem);
        viewMenu.add(zoomInMenuItem);
        viewMenu.add(zoomOutMenuItem);
        return viewMenu;
    }

    private JMenu buildNavigationMenu() {
        JMenu navigationMenu = new JMenu("Navigation");
        JMenuItem nextImageItem = new JMenuItem("Next (→)");
        JMenuItem previousImageItem = new JMenuItem("Prev (←)");
        JMenuItem firstImageItem = new JMenuItem("first (⇱)");
        JMenuItem lastImageItem = new JMenuItem("last (⇲)");

        nextImageItem.addActionListener(e -> showNextImage());
        previousImageItem.addActionListener(e -> showPreviousImage());
        firstImageItem.addActionListener(e -> showFirstImage());
        lastImageItem.addActionListener(e -> showLastImage());

        navigationMenu.add(nextImageItem);
        navigationMenu.add(previousImageItem);
        navigationMenu.add(firstImageItem);
        navigationMenu.add(lastImageItem);

        return navigationMenu;
    }

    private JMenu buildOptionsMenu() {
        JMenu optionsMenu = new JMenu("Options");
        JMenu colorSchemeMenu = new JMenu("Color scheme");
        JMenuItem nimbusThemeMenuItem = new JMenuItem("Nimbus theme");
        JMenuItem metalThemeMenuItem = new JMenuItem("Metal theme");
        JMenuItem systemThemeMenuItem = new JMenuItem("System theme");

        JMenuItem toggleFullscreenMenuItem = new JMenuItem("Toggle Fullscreen (F)");

        nimbusThemeMenuItem.addActionListener(e -> {
            setDarkTheme();
            redrawComponent(this);
        });
        metalThemeMenuItem.addActionListener(e -> {
            setMetalTheme();
            redrawComponent(this);
        });
        systemThemeMenuItem.addActionListener(e -> {
            setSystemTheme();
            redrawComponent(this);
        });
        toggleFullscreenMenuItem.addActionListener(e -> toggleFullscreen());

        colorSchemeMenu.add(nimbusThemeMenuItem);
        colorSchemeMenu.add(metalThemeMenuItem);
        colorSchemeMenu.add(systemThemeMenuItem);
        optionsMenu.add(colorSchemeMenu);
        optionsMenu.add(toggleFullscreenMenuItem);

        return optionsMenu;
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About");

        aboutMenuItem.addActionListener(e -> showAboutDialog(this));
        helpMenu.add(aboutMenuItem);

        return helpMenu;
    }

    private void chooseFile() {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(null);
        }
        String root = /*currentFile == null ? */ System.getProperty("user.home")/* : currentFile.getParent().toString()*/;
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
        setTitle(picture.name());
        imagePanel.drawImage(picture.image());
    }

    private void toggleFullscreen() {
        if (fullscreen) {
            setVisible(false);
            dispose();
            setUndecorated(false);
            graphicsDevice.setFullScreenWindow(null);
            southPanel.setVisible(true);
            menuBar.setVisible(true);
            setVisible(true);
        } else {
            setVisible(false);
            dispose();
            setUndecorated(true);
            graphicsDevice.setFullScreenWindow(this);
            southPanel.setVisible(false);
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
