package org.dustyroom.ui.components;

import org.dustyroom.ui.components.listeners.CustomListener;
import org.dustyroom.ui.components.listeners.ThemeChangeListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class MenuBar extends JMenuBar implements ActionListener {
    private final CustomListener openFileListener;
    private final CustomListener fitModeListener;
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
            CustomListener fitModeListener,
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
        this.fitModeListener = fitModeListener;
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
        JMenuItem openMenuItem = new JMenuItem("Open (O)");
        JMenuItem exitMenuItem = new JMenuItem("Exit (Q)");

        listenerMap.put(openMenuItem, openFileListener);
        listenerMap.put(exitMenuItem, exitListener);

        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private JMenu buildViewMenu() {
        JMenu viewMenu = new JMenu("View");
        JMenuItem fitMenuItem = new JMenuItem("Fit height mode (h)");
        JMenuItem zoomInMenuItem = new JMenuItem("Zoom In (+)");
        JMenuItem zoomOutMenuItem = new JMenuItem("Zoom out (-)");

        listenerMap.put(fitMenuItem, fitModeListener);
        listenerMap.put(zoomInMenuItem, zoomInListener);
        listenerMap.put(zoomOutMenuItem, zoomOutListener);

        viewMenu.add(fitMenuItem);
        viewMenu.add(zoomInMenuItem);
        viewMenu.add(zoomOutMenuItem);
        return viewMenu;
    }

    private JMenu buildNavigationMenu() {
        JMenu navigationMenu = new JMenu("Navigation");
        JMenuItem nextImageItem = new JMenuItem("Next (→)");
        JMenuItem previousImageItem = new JMenuItem("Prev (←)");
        JMenuItem firstImageItem = new JMenuItem("First (⇱)");
        JMenuItem lastImageItem = new JMenuItem("Last (⇲)");
        JMenuItem prevVolumeItem = new JMenuItem("Prev volume");
        JMenuItem nextVolumeItem = new JMenuItem("Next volume");

        listenerMap.put(nextImageItem, nextFileListener);
        listenerMap.put(previousImageItem, prevFileListener);
        listenerMap.put(firstImageItem, firstFileListener);
        listenerMap.put(lastImageItem, lastFileListener);
        listenerMap.put(prevVolumeItem, prevVolumeListener);
        listenerMap.put(nextVolumeItem, nextVolumeListener);

        navigationMenu.add(nextImageItem);
        navigationMenu.add(previousImageItem);
        navigationMenu.add(firstImageItem);
        navigationMenu.add(lastImageItem);
        navigationMenu.add(prevVolumeItem);
        navigationMenu.add(nextVolumeItem);

        return navigationMenu;
    }

    private JMenu buildOptionsMenu() {
        JMenu optionsMenu = new JMenu("Options");
        JMenu colorSchemeMenu = new JMenu("Color scheme");
        JMenuItem nimbusThemeMenuItem = new JMenuItem("Nimbus theme");
        JMenuItem metalThemeMenuItem = new JMenuItem("Metal theme");
        JMenuItem systemThemeMenuItem = new JMenuItem("System theme");
        JMenuItem toggleFullscreenMenuItem = new JMenuItem("Toggle Fullscreen (F)");

        listenerMap.put(toggleFullscreenMenuItem, toggleFullscreenListener);

        nimbusThemeMenuItem.addActionListener(a -> themeChangeListener.setNimbusTheme());
        metalThemeMenuItem.addActionListener(a -> themeChangeListener.setMetalTheme());
        systemThemeMenuItem.addActionListener(a -> themeChangeListener.setSystemTheme());

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

        listenerMap.put(aboutMenuItem, showAboutListener);

        helpMenu.add(aboutMenuItem);

        return helpMenu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JMenuItem clicked = (JMenuItem) e.getSource();
        listenerMap.keySet()
                .stream()
                .filter(k -> k == clicked)
                .findFirst()
                .ifPresent(pressed -> listenerMap.get(pressed).performAction());
        SwingUtilities.getWindowAncestor(this).requestFocus();
    }
}
