package org.dustyroom.ui;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.ui.actions.DefaultViewerActions;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.components.KeyBindings;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.window.WindowChrome;
import org.dustyroom.ui.window.WindowStateController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

@Slf4j
public class ImageViewer extends JFrame {
    private static final int RESIZE_RENDER_DELAY_MS = 80;

    private final ViewerController viewerController;
    private final ViewerActions viewerActions;
    private final KeyBindings keyBindings;
    private final MenuBar menuBar;
    private final NavigationPanel navigationPanel;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final Timer resizeRenderTimer;
    private final WindowChrome windowChrome;
    private final WindowStateController windowStateController;

    public ImageViewer() {
        setTitle("Manga Reader");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setPreferredSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(480, 360));

        imageComponent = new ImageComponent();
        scrollPane = new JScrollPane(imageComponent);
        viewerController = new ViewerController(this, imageComponent, scrollPane);
        viewerActions = new DefaultViewerActions(viewerController);
        keyBindings = new KeyBindings();
        resizeRenderTimer = new Timer(RESIZE_RENDER_DELAY_MS, e -> applyResizeRender());
        resizeRenderTimer.setRepeats(false);

        menuBar = new MenuBar(viewerActions);
        navigationPanel = new NavigationPanel(viewerActions);
        windowChrome = new WindowChrome(scrollPane, menuBar, navigationPanel);
        setContentPane(windowChrome);
        windowStateController = new WindowStateController(this, windowChrome);
        viewerController.attachWindowStateController(windowStateController);

        initializeUI();
        viewerController.syncThemeSurfaces();
    }

    private void initializeUI() {
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
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

    public void showWindow() {
        setVisible(true);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }
}
