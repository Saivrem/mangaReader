package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.*;

public class MenuBar extends JMenuBar {
    public MenuBar (ViewerActions actions) {
        JMenu fileMenu = new JMenu("File");
        JMenu viewMenu = new JMenu("View");
        JMenu navigationMenu = new JMenu("Navigation");
        JMenu helpMenu = new JMenu("Help");
        JMenu optionsMenu = new JMenu("Options");
        JMenu colorSchemeMenu = new JMenu("Color scheme");
        JMenu readingModeMenu = new JMenu("Reading mode");

        // @formatter:off
        addMenuItem(fileMenu, "Open (O)"                          , actions.openFile());
        addMenuItem(fileMenu, "Exit (Q)"                          , actions.exit());

        addMenuItem(viewMenu, "Fit height mode (h)"               , actions.fitHeight());
        addMenuItem(viewMenu, "Fit width mode (w)"                , actions.fitWidth());
        addMenuItem(viewMenu, "Fit screen mode (s)"               , actions.fitScreen());
        addMenuItem(viewMenu, "Toggle two-page mode (p)"          , actions.toggleTwoPageMode());
        addMenuItem(viewMenu, "Zoom In (+)"                       , actions.zoomIn());
        addMenuItem(viewMenu, "Zoom out (-)"                      , actions.zoomOut());

        addMenuItem(navigationMenu, "Next (→)"                    , actions.nextImage());
        addMenuItem(navigationMenu, "Prev (←)"                    , actions.prevImage());
        addMenuItem(navigationMenu, "First (⇱)"                   , actions.firstImage());
        addMenuItem(navigationMenu, "Last (⇲)"                    , actions.lastImage());
        addMenuItem(navigationMenu, "Prev volume"                 , actions.prevVolume());
        addMenuItem(navigationMenu, "Next volume"                 , actions.nextVolume());

        addMenuItem(colorSchemeMenu, "Nimbus theme"               , actions.setNimbusTheme());
        addMenuItem(colorSchemeMenu, "Metal theme"                , actions.setMetalTheme());
        addMenuItem(colorSchemeMenu, "System theme"               , actions.setSystemTheme());
        addMenuItem(readingModeMenu, "Comics (left to right) (c)" , actions.setComicsReadingMode());
        addMenuItem(readingModeMenu, "Manga (right to left) (m)"  , actions.setMangaReadingMode());

        addMenuItem(optionsMenu, "Toggle Fullscreen (F)"          , actions.toggleFullscreen());
        addMenuItem(helpMenu, "About"                             , actions.showAbout());
        // @formatter:on

        optionsMenu.add(colorSchemeMenu);
        optionsMenu.add(readingModeMenu);

        add(fileMenu);
        add(viewMenu);
        add(navigationMenu);
        add(optionsMenu);
        add(helpMenu);
    }

    private void addMenuItem(JMenu menu, String itemName, Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setText(itemName);
        menu.add(item);
    }
}
