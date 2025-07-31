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
    private final CustomListener exitListener = () -> System.exit(0);

    private final Map<JMenuItem, CustomListener> listenerMap = new HashMap<>();

    public MenuBar init(CustomListener openFileListener,
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
                        CustomListener showAboutListener) {
        JMenu fileMenu = new JMenu("File");
        // @formatter:off
        addMenuItem(fileMenu, "Open (O)" , openFileListener);
        addMenuItem(fileMenu, "Exit (Q)" , exitListener);
        // @formatter:on
        add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        // @formatter:off
        addMenuItem(viewMenu, "Fit height mode (h)" , fitHeightModeListener);
        addMenuItem(viewMenu, "Fit width mode (w)"  , fitWidthModeListener);
        addMenuItem(viewMenu, "Zoom In (+)"         , zoomInListener);
        addMenuItem(viewMenu, "Zoom out (-)"        , zoomOutListener);
        // @formatter:on
        add(viewMenu);

        JMenu navigationMenu = new JMenu("Navigation");
        // @formatter:off
        addMenuItem(navigationMenu, "Next (→)"      , nextFileListener);
        addMenuItem(navigationMenu, "Prev (←)"      , prevFileListener);
        addMenuItem(navigationMenu, "First (⇱)"     , firstFileListener);
        addMenuItem(navigationMenu, "Last (⇲)"      , lastFileListener);
        addMenuItem(navigationMenu, "Prev volume"   , prevVolumeListener);
        addMenuItem(navigationMenu, "Next volume"   , nextVolumeListener);
        // @formatter:on
        add(navigationMenu);

        JMenu optionsMenu = new JMenu("Options");
        JMenu colorSchemeMenu = new JMenu("Color scheme");
        // @formatter:off
        addThemeMenuItem(colorSchemeMenu, "Nimbus theme"     , themeChangeListener::setNimbusTheme);
        addThemeMenuItem(colorSchemeMenu, "Metal theme"      , themeChangeListener::setMetalTheme);
        addThemeMenuItem(colorSchemeMenu, "System theme"     , themeChangeListener::setSystemTheme);

        addMenuItem(optionsMenu, "Toggle Fullscreen (F)" , toggleFullscreenListener);
        // @formatter:on
        optionsMenu.add(colorSchemeMenu);
        add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        // @formatter:off
        addMenuItem(helpMenu, "About" , showAboutListener);
        // @formatter:on
        add(helpMenu);

        listenerMap.keySet().forEach(k -> k.addActionListener(this));

        return this;
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
