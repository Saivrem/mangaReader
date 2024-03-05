package org.dustyroom.ui.components;

import org.dustyroom.ui.components.listeners.*;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    private final OpenFileListener openFileListener;
    private final FitModeListener fitModeListener;
    private final ZoomInListener zoomInListener;
    private final ZoomOutListener zoomOutListener;
    private final NextFileListener nextFileListener;
    private final PrevFileListener prevFileListener;
    private final FirstFileListener firstFileListener;
    private final LastFileListener lastFileListener;
    private final ToggleFullscreenListener toggleFullscreenListener;
    private final ThemeChangeListener themeChangeListener;
    private final ShowAboutListener showAboutListener;

    public MenuBar(
            OpenFileListener openFileListener,
            FitModeListener fitModeListener,
            ZoomInListener zoomInListener,
            ZoomOutListener zoomOutListener,
            NextFileListener nextFileListener,
            PrevFileListener prevFileListener,
            FirstFileListener firstFileListener,
            LastFileListener lastFileListener,
            ToggleFullscreenListener toggleFullscreenListener,
            ThemeChangeListener themeChangeListener,
            ShowAboutListener showAboutListener
    ) {
        this.openFileListener = openFileListener;
        this.fitModeListener = fitModeListener;
        this.zoomInListener = zoomInListener;
        this.zoomOutListener = zoomOutListener;
        this.nextFileListener = nextFileListener;
        this.prevFileListener = prevFileListener;
        this.firstFileListener = firstFileListener;
        this.lastFileListener = lastFileListener;
        this.toggleFullscreenListener = toggleFullscreenListener;
        this.themeChangeListener = themeChangeListener;
        this.showAboutListener = showAboutListener;

        add(buildFileMenu());
        add(buildViewMenu());
        add(buildNavigationMenu());
        add(buildOptionsMenu());
        add(buildHelpMenu());
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open (O)");
        JMenuItem exitMenuItem = new JMenuItem("Exit (Q)");

        openMenuItem.addActionListener(e -> openFileListener.open());
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

        fitMenuItem.addActionListener(e -> fitModeListener.fitMode());

        zoomInMenuItem.addActionListener(e -> zoomInListener.zoomIn());
        zoomOutMenuItem.addActionListener(e -> zoomOutListener.zoomOut());

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

        nextImageItem.addActionListener(e -> nextFileListener.next());
        previousImageItem.addActionListener(e -> prevFileListener.prev());
        firstImageItem.addActionListener(e -> firstFileListener.first());
        lastImageItem.addActionListener(e -> lastFileListener.last());

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

        nimbusThemeMenuItem.addActionListener(e -> themeChangeListener.setNimbusTheme());
        metalThemeMenuItem.addActionListener(e -> themeChangeListener.setMetalTheme());
        systemThemeMenuItem.addActionListener(e -> themeChangeListener.setSystemTheme());
        toggleFullscreenMenuItem.addActionListener(e -> toggleFullscreenListener.toggleFullscreen());

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

        aboutMenuItem.addActionListener(e -> showAboutListener.showAbout());
        helpMenu.add(aboutMenuItem);

        return helpMenu;
    }
}
