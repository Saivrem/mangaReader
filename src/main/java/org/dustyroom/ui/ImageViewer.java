package org.dustyroom.ui;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.filewalking.FileTreeIterator;
import org.dustyroom.be.filewalking.MangaFileVisitor;
import org.dustyroom.ui.panels.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.PathUtils.getFileName;
import static org.dustyroom.be.utils.PathUtils.isImage;
import static org.dustyroom.be.utils.UiUtils.*;
import static org.dustyroom.ui.utils.DialogUtils.showAboutDialog;

@Slf4j
public class ImageViewer extends JFrame {

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
    private FileTreeIterator fileTreeIterator;
    @Getter
    private Path currentFile;

    public ImageViewer(FileTreeIterator fileTreeIterator) {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        this.fileTreeIterator = fileTreeIterator;
        initializeUI();
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

        if (fileTreeIterator != null) {
            showNextImage();
        }

        setVisible(true);
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
                    case KeyEvent.VK_ESCAPE:
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
        JMenu navigationMenu = buildNavigationMenu();
        JMenu optionsMenu = buildOptionsMenu();
        JMenu helpMenu = buildHelpMenu();

        menuBar.add(fileMenu);
        menuBar.add(navigationMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");

        openMenuItem.addActionListener(e -> chooseFile());
        fileMenu.add(openMenuItem);
        return fileMenu;
    }

    private JMenu buildNavigationMenu() {
        JMenu navigationMenu = new JMenu("Navigation");
        JMenuItem nextImageItem = new JMenuItem("Next →");
        JMenuItem previousImageItem = new JMenuItem("Prev ←");
        JMenuItem firstImageItem = new JMenuItem("first ⇱");
        JMenuItem lastImageItem = new JMenuItem("last ⇲");

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
        JMenuItem systemThemeMenuItem = new JMenuItem("System theme");

        nimbusThemeMenuItem.addActionListener(e -> {
            setDarkTheme();
            redrawComponent(this);
        });
        systemThemeMenuItem.addActionListener(e -> {
            setSystemTheme();
            redrawComponent(this);
        });

        colorSchemeMenu.add(nimbusThemeMenuItem);
        colorSchemeMenu.add(systemThemeMenuItem);
        optionsMenu.add(colorSchemeMenu);

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
        String root = currentFile == null ? System.getProperty("user.home") : currentFile.getParent().toString();
        JFileChooser fileChooser = new JFileChooser(root);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(FILES_AND_DIRECTORIES);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            Path levelUp = selectedFile.getParent().getParent();

            MangaFileVisitor mangaFileVisitor = new MangaFileVisitor();
            try {
                if (Files.isRegularFile(selectedFile)) {
                    Files.walkFileTree(levelUp, Set.of(FileVisitOption.FOLLOW_LINKS), 2, mangaFileVisitor);
                } else {
                    Files.walkFileTree(selectedFile, mangaFileVisitor);
                }
            } catch (IOException ioException) {
                log.error("Can't walk {}, application will be closed", selectedFile);
                System.exit(1);
            }
            fileTreeIterator = new FileTreeIterator(mangaFileVisitor.getTree(), selectedFile);
            currentFile = fileTreeIterator.next();
            updateImagePanel();
        }
        requestFocus();
    }

    private void updateImagePanel() {
        if (isImage(currentFile)) {
            setTitle(String.format(String.format("%s - %s - %s - %s",
                    getFileName(currentFile, 4),
                    getFileName(currentFile, 3),
                    getFileName(currentFile, 2),
                    getFileName(currentFile, 1)
            )));
            try {
                imagePanel.setImage(ImageIO.read(Files.newInputStream(currentFile)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.repaint();
        }
        requestFocus();
    }

    private void showNextImage() {
        Path next = fileTreeIterator.next();
        if (next == null) return;
        if (next.equals(currentFile)) {
            next = fileTreeIterator.next();
            currentFile = next != null ? next : currentFile;
        } else {
            currentFile = next;
        }
        updateImagePanel();
    }

    private void showPreviousImage() {
        Path previous = fileTreeIterator.previous();
        if (previous == null) return;
        if (previous.equals(currentFile)) {
            previous = fileTreeIterator.previous();
            currentFile = previous != null ? previous : currentFile;
        } else {
            currentFile = previous;
        }
        updateImagePanel();
    }

    private void showFirstImage() {
        currentFile = fileTreeIterator.getFirst();
        updateImagePanel();
    }

    private void showLastImage() {
        currentFile = fileTreeIterator.getLast();
        updateImagePanel();
    }
}
