package org.dustyroom.ui;

import org.dustyroom.ui.actions.DefaultViewerActions;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.components.KeyBindings;
import org.dustyroom.ui.components.MenuBar;
import org.dustyroom.ui.components.NavigationPanel;
import org.dustyroom.ui.components.ReaderSurface;
import org.dustyroom.ui.components.FileDropHandler;
import org.dustyroom.ui.utils.DialogUtils;
import org.dustyroom.persistence.ReaderSettings;
import org.dustyroom.persistence.ReaderStore;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class ImageViewer extends JFrame {
    private static final int RESIZE_RENDER_DELAY_MS = 80;

    private final ViewerController viewerController;
    private final MenuBar menuBar;
    private final NavigationPanel navigationPanel;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final ReaderSurface readerSurface;
    private final Timer resizeRenderTimer;

    public ImageViewer(ReaderStore store, ReaderSettings settings) {
        setTitle("Yonaka Reader");
        setIconImages(BrandAssets.windowIcons());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(1100, 760));
        setMinimumSize(new Dimension(940, 560));

        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        imageComponent = new ImageComponent();
        imageComponent.restoreView(settings.fitMode(), settings.zoom());
        scrollPane = new JScrollPane(imageComponent);
        viewerController = new ViewerController(this, graphicsDevice, imageComponent, scrollPane, store, settings);
        ViewerActions viewerActions = new DefaultViewerActions(viewerController);
        resizeRenderTimer = new Timer(RESIZE_RENDER_DELAY_MS, e -> applyResizeRender());
        resizeRenderTimer.setRepeats(false);

        menuBar = new MenuBar(viewerActions, viewerController::setTheme);
        navigationPanel = new NavigationPanel(viewerActions);
        readerSurface = new ReaderSurface(scrollPane, viewerActions.openFile());
        viewerController.attachPanels(menuBar, navigationPanel, readerSurface, viewerActions);
        PlatformUi.installApplicationHandlers(viewerController);

        initializeUI(viewerActions);
        new FileDropHandler(viewerController::openFile, message -> DialogUtils.showError(this, message))
                .installOn(getRootPane());
        viewerController.configureViewerSurfaces();
        setVisible(true);
    }

    private void initializeUI(ViewerActions viewerActions) {
        setLayout(new BorderLayout());

        // PlatformUi configures screen-menu integration before AWT starts.
        setJMenuBar(menuBar);
        add(viewerController.createThumbnailPanel(), BorderLayout.WEST);
        add(readerSurface, BorderLayout.CENTER);
        add(navigationPanel, BorderLayout.SOUTH);

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

        new KeyBindings().init(getRootPane(), viewerActions);

        pack();
        setLocationRelativeTo(null);
        setFocusable(true);
        requestFocusInWindow();
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
