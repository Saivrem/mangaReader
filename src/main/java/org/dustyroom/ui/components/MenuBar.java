package org.dustyroom.ui.components;

import org.dustyroom.ui.components.listeners.CustomListener;
import org.dustyroom.ui.components.listeners.ThemeChangeListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import static org.dustyroom.ui.utils.UiUtils.performAction;

public class MenuBar extends JMenuBar implements ActionListener {
    private final CustomListener openFileListener;
    private final CustomListener fitHeightModeListener;
    private final CustomListener fitWidthModeListener;
    private final CustomListener zoomInListener;
    private final CustomListener zoomOutListener;
    private final CustomListener nextFileListener;
    private final CustomListener prevFileListener;
    private final CustomListener firstFileListener;
    private final CustomListener lastFileListener;
    private final CustomListener nextVolumeListener;
    private final CustomListener prevVolumeListener;
    private final CustomListener toggleFullscreenListener;
    private final ThemeChangeListener themeChangeListener;
    private final CustomListener showAboutListener;
    private final CustomListener exitListener = () -> System.exit(0);

    private final Map<JMenuItem, CustomListener> listenerMap = new HashMap<>();

    public MenuBar(
            CustomListener openFileListener,
            CustomListener fitHeightModeListener,
            CustomListener fitWidthModeListener,
            CustomListener zoomInListener,
            CustomListener zoomOutListener,
            CustomListener nextFileListener,
            CustomListener prevFileListener,
            CustomListener firstFileListener,
            CustomListener lastFileListener,
            CustomListener nextVolumeListener,
            CustomListener prevVolumeListener,
            CustomListener toggleFullscreenListener,
            ThemeChangeListener themeChangeListener,
            CustomListener showAboutListener
    ) {
        this.openFileListener = openFileListener;
        this.fitHeightModeListener = fitHeightModeListener;
        this.fitWidthModeListener = fitWidthModeListener;
        this.zoomInListener = zoomInListener;
        this.zoomOutListener = zoomOutListener;
        this.nextFileListener = nextFileListener;
        this.prevFileListener = prevFileListener;
        this.firstFileListener = firstFileListener;
        this.lastFileListener = lastFileListener;
        this.nextVolumeListener = nextVolumeListener;
        this.prevVolumeListener = prevVolumeListener;
        this.toggleFullscreenListener = toggleFullscreenListener;
        this.themeChangeListener = themeChangeListener;
        this.showAboutListener = showAboutListener;

        add(buildFileMenu());
        add(buildViewMenu());
        add(buildNavigationMenu());
        add(buildOptionsMenu());
        add(buildHelpMenu());

        listenerMap.keySet().forEach(k -> k.addActionListener(this));
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        // @formatter:off
        addMenuItem(fileMenu, "Open (O)" , openFileListener);
        addMenuItem(fileMenu, "Exit (Q)" , exitListener);
        // @formatter:on
        return fileMenu;
    }

    private JMenu buildViewMenu() {
        JMenu viewMenu = new JMenu("View");
        // @formatter:off
        addMenuItem(viewMenu, "Fit height mode (h)" , fitHeightModeListener);
        addMenuItem(viewMenu, "Fit width mode (w)"  , fitWidthModeListener);
        addMenuItem(viewMenu, "Zoom In (+)"         , zoomInListener);
        addMenuItem(viewMenu, "Zoom out (-)"        , zoomOutListener);
        // @formatter:on
        return viewMenu;
    }

    private JMenu buildNavigationMenu() {
        JMenu navigationMenu = new JMenu("Navigation");
        // @formatter:off
        addMenuItem(navigationMenu, "Next (→)"      , nextFileListener);
        addMenuItem(navigationMenu, "Prev (←)"      , prevFileListener);
        addMenuItem(navigationMenu, "First (⇱)"     , firstFileListener);
        addMenuItem(navigationMenu, "Last (⇲)"      , lastFileListener);
        addMenuItem(navigationMenu, "Prev volume"   , prevVolumeListener);
        addMenuItem(navigationMenu, "Next volume"   , nextVolumeListener);
        // @formatter:on
        return navigationMenu;
    }

    private JMenu buildOptionsMenu() {
        JMenu optionsMenu = new JMenu("Options");
        JMenu colorSchemeMenu = new JMenu("Color scheme");
        // @formatter:off
        addThemeMenuItem(colorSchemeMenu, "Nimbus theme" , themeChangeListener::setNimbusTheme);
        addThemeMenuItem(colorSchemeMenu, "Metal theme"  , themeChangeListener::setMetalTheme);
        addThemeMenuItem(colorSchemeMenu, "System theme" , themeChangeListener::setSystemTheme);
        // @formatter:on
        // @formatter:off
        addMenuItem(optionsMenu, "Toggle Fullscreen (F)" , toggleFullscreenListener);
        // @formatter:on
        optionsMenu.add(colorSchemeMenu);
        return optionsMenu;
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        // @formatter:off
        addMenuItem(helpMenu, "About" , showAboutListener);
        // @formatter:on
        return helpMenu;
    }

    private void addMenuItem(JMenu menu, String itemName, CustomListener listener) {
        JMenuItem item = new JMenuItem(itemName);
        listenerMap.put(item, listener);
        menu.add(item);
    }

    private void addThemeMenuItem(JMenu menu, String title, Runnable themeAction) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(e -> themeAction.run());
        menu.add(item);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction(e, this, listenerMap);
    }
}
