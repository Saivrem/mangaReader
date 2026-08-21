package org.dustyroom.ui;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.actions.DefaultViewerActions;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.components.KeyBindings;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import static org.dustyroom.ui.utils.UiUtils.setDarkTheme;

@Slf4j
public class ImageViewer extends JFrame {
    private static final int RESIZE_RENDER_DELAY_MS = 80;

    private final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    private final GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
    private final ViewerController viewerController;
    private final ViewerActions viewerActions;
    private final KeyBindings keyBindings;
    private final MenuBar menuBar;
    private final NavigationPanel navigationPanel;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final Timer resizeRenderTimer;

    public ImageViewer() {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setDarkTheme();

        imageComponent = new ImageComponent();
        scrollPane = new JScrollPane(imageComponent);
        viewerController = new ViewerController(this, graphicsDevice, imageComponent, scrollPane);
        viewerActions = new DefaultViewerActions(viewerController);
        keyBindings = new KeyBindings();
        resizeRenderTimer = new Timer(RESIZE_RENDER_DELAY_MS, e -> applyResizeRender());
        resizeRenderTimer.setRepeats(false);

        menuBar = new MenuBar(viewerActions);
        navigationPanel = new NavigationPanel(viewerActions);
        viewerController.attachPanels(menuBar, navigationPanel);

        initializeUI();
        viewerController.syncThemeSurfaces();
        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        setJMenuBar(menuBar);
        add(scrollPane, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (imageComponent.getFitMode() != ImageComponent.FitMode.ORIGINAL) {
                    resizeRenderTimer.restart();
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                viewerController.exit();
            }
        });

        setupControls();

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocusInWindow();
    }

    private void setupControls() {
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        keyBindings.init(getRootPane(), viewerActions);
    }

    private void applyResizeRender() {
        imageComponent.updateScale();
        imageComponent.revalidate();
        imageComponent.repaint();
    }

    public void openFile(File file) {
        viewerController.openFile(file);
    }
}
