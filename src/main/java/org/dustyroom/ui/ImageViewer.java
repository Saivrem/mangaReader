package org.dustyroom.ui;

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

import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.PathUtils.getFileName;
import static org.dustyroom.be.utils.PathUtils.isNotImage;

@Slf4j
public class ImageViewer extends JFrame {

    // Buttons
    private final JPanel southPanel = new JPanel(new FlowLayout());
    private final JButton openButton;
    private final JButton nextButton;
    private final JButton prevButton;

    // Panels
    private final ImagePanel imagePanel;

    // Other
    private FileTreeIterator fileTreeIterator;
    private Path currentFile;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        imagePanel = new ImagePanel();
        openButton = new JButton("Open Image");
        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");

        initializeUI();
    }

    private void initializeUI() {
        setContentPane(imagePanel);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imagePanel.repaint();
            }
        });

        setLayout(new BorderLayout());
        add(createMenuBar(), BorderLayout.NORTH);

        southPanel.add(prevButton);
        southPanel.add(openButton);
        southPanel.add(nextButton);
        add(southPanel, BorderLayout.SOUTH);

        prevButton.addActionListener(e -> showPreviousImage());
        openButton.addActionListener(e -> chooseFile());
        nextButton.addActionListener(e -> showNextImage());

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
                        showNextImage();
                        break;
                    case KeyEvent.VK_LEFT:
                        showPreviousImage();
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

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocus();

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open");

        openMenuItem.addActionListener(e -> chooseFile());

        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    private void chooseFile() {
        String root = currentFile == null ? System.getProperty("user.home") : currentFile.getParent().toString();
        JFileChooser fileChooser = new JFileChooser(root);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);

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
    }

    private void updateImagePanel() {
        if (isNotImage(currentFile)) {
            return;
        }

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
        requestFocus();
    }

    private void showNextImage() {
        Path next = fileTreeIterator.next();
        if (next == null) return;
        if (currentFile.equals(next)) {
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
        if (currentFile.equals(previous)) {
            previous = fileTreeIterator.previous();
            currentFile = previous != null ? previous : currentFile;
        } else {
            currentFile = previous;
        }
        updateImagePanel();
    }
}
