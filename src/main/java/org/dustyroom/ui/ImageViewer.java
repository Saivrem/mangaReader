package org.dustyroom.ui;

import org.dustyroom.ui.panels.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageViewer extends JFrame {

    // Buttons
    private final JPanel southPanel = new JPanel(new FlowLayout());
    private final JButton openButton;
    private final JButton nextButton;
    private final JButton prevButton;

    // Panels
    private final ImagePanel imagePanel;

    // Other

    File[] files;
    private int currentIndex;
    private final static String[] extensions = {"jpg", "jpeg", "png", "gif"};

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
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", extensions);
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            files = selectedFile.getParentFile().listFiles((dir, name) -> Arrays.stream(extensions).anyMatch(ext -> name.toLowerCase().endsWith(ext)));
            currentIndex = 0;
            if (files != null && files.length > 0) {
                Arrays.sort(files);
                updateImagePanel(selectedFile);
            }
        }
    }

    private void updateImagePanel(File selectedFile) {
        if (selectedFile != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(selectedFile)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        if (currentIndex >= 0 && currentIndex < files.length) {
            File currentFile = files[currentIndex];
            setTitle(currentFile.getName());
            try {
                imagePanel.setImage(ImageIO.read(currentFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.repaint();
            requestFocus();
        }
    }

    private void showNextImage() {
        currentIndex = (currentIndex + 1) % files.length;
        updateImagePanel(null);
    }

    private void showPreviousImage() {
        currentIndex = (currentIndex - 1 + files.length) % files.length;
        updateImagePanel(null);
    }
}
