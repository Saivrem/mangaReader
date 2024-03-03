package org.dustyroom.ui.components;

import javax.swing.*;

import static org.dustyroom.be.utils.UiUtils.*;
import static org.dustyroom.ui.utils.DialogUtils.showAboutDialog;

public class MenuBar extends JMenuBar {

    private final JMenu fileMenu;
    private final JMenu viewMenu;
    private final JMenu navigationMenu;
    private final JMenu optionsMenu;
    private final JMenu helpMenu;
    private final OptionsMenuListener optionsMenuListener;

    public MenuBar(OptionsMenuListener optionsMenuListener) {
        // Probably I am going to remove JMenu variables but let them stay
        // in case I decide to actually use them from somewhere;
        this.optionsMenuListener = optionsMenuListener;

        fileMenu = buildFileMenu();
        viewMenu = buildViewMenu();
        navigationMenu = buildNavigationMenu();
        optionsMenu = buildOptionsMenu();
        helpMenu = buildHelpMenu();

        add(fileMenu);
        add(viewMenu);
        add(navigationMenu);
        add(optionsMenu);
        add(helpMenu);
    }

    private JMenu buildFileMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openMenuItem = new JMenuItem("Open (O)");
        JMenuItem exitMenuItem = new JMenuItem("Exit (Q)");

        openMenuItem.addActionListener(e -> chooseFile());
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

        fitMenuItem.addActionListener(e -> fitMode());

        zoomInMenuItem.addActionListener(e -> zoomIn());
        zoomOutMenuItem.addActionListener(e -> zoomOut());

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

        nextImageItem.addActionListener(e -> showNextImage());
        previousImageItem.addActionListener(e -> showPreviousImage());
        firstImageItem.addActionListener(e -> showFirstImage());
        lastImageItem.addActionListener(e -> showLastImage());

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

        nimbusThemeMenuItem.addActionListener(e -> {
            setDarkTheme();
            redrawComponent(this);
        });
        metalThemeMenuItem.addActionListener(e -> {
            setMetalTheme();
            redrawComponent(this);
        });
        systemThemeMenuItem.addActionListener(e -> {
            setSystemTheme();
            redrawComponent(this);
        });
        toggleFullscreenMenuItem.addActionListener(e -> optionsMenuListener.toggleFullscreen());

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

        aboutMenuItem.addActionListener(e -> showAboutDialog(this));
        helpMenu.add(aboutMenuItem);

        return helpMenu;
    }
}
