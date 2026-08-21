package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class MenuBar extends JMenuBar {
    public MenuBar(ViewerActions actions) {
        setName("mainMenuBar");
        getAccessibleContext().setAccessibleName("Main menu");
        getAccessibleContext().setAccessibleDescription("Application commands and settings");

        JMenu fileMenu = menu("File", KeyEvent.VK_F, "file");
        JMenu viewMenu = menu("View", KeyEvent.VK_V, "view");
        JMenu navigationMenu = menu("Navigation", KeyEvent.VK_N, "navigation");
        JMenu helpMenu = menu("Help", KeyEvent.VK_H, "help");
        JMenu optionsMenu = menu("Options", KeyEvent.VK_O, "options");
        JMenu colorSchemeMenu = menu("Color Theme", KeyEvent.VK_C, "colorTheme");
        JMenu readingModeMenu = menu("Reading Mode", KeyEvent.VK_R, "readingMode");

        addMenuItem(fileMenu, actions.openFile());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, actions.exit());

        addMenuItem(viewMenu, actions.fitHeight());
        addMenuItem(viewMenu, actions.fitWidth());
        addMenuItem(viewMenu, actions.fitScreen());
        viewMenu.addSeparator();
        addMenuItem(viewMenu, actions.zoomIn());
        addMenuItem(viewMenu, actions.zoomOut());

        addMenuItem(navigationMenu, actions.nextImage());
        addMenuItem(navigationMenu, actions.prevImage());
        addMenuItem(navigationMenu, actions.firstImage());
        addMenuItem(navigationMenu, actions.lastImage());
        navigationMenu.addSeparator();
        addMenuItem(navigationMenu, actions.prevVolume());
        addMenuItem(navigationMenu, actions.nextVolume());

        ButtonGroup themeGroup = new ButtonGroup();
        addRadioMenuItem(colorSchemeMenu, themeGroup, actions.setDarkTheme());
        addRadioMenuItem(colorSchemeMenu, themeGroup, actions.setLightTheme());

        ButtonGroup readingModeGroup = new ButtonGroup();
        addRadioMenuItem(readingModeMenu, readingModeGroup, actions.setComicsReadingMode());
        addRadioMenuItem(readingModeMenu, readingModeGroup, actions.setMangaReadingMode());
        readingModeMenu.addSeparator();
        addCheckMenuItem(readingModeMenu, actions.toggleTwoPageMode());

        addCheckMenuItem(optionsMenu, actions.toggleFullscreen());
        optionsMenu.addSeparator();
        optionsMenu.add(colorSchemeMenu);
        optionsMenu.add(readingModeMenu);

        addMenuItem(helpMenu, actions.showAbout());

        add(fileMenu);
        add(viewMenu);
        add(navigationMenu);
        add(optionsMenu);
        add(helpMenu);
    }

    private void addMenuItem(JMenu menu, Action action) {
        JMenuItem item = new JMenuItem(action);
        configureMenuItem(item, action);
        menu.add(item);
    }

    private void addCheckMenuItem(JMenu menu, Action action) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        configureMenuItem(item, action);
        menu.add(item);
    }

    private void addRadioMenuItem(JMenu menu, ButtonGroup group, Action action) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
        configureMenuItem(item, action);
        group.add(item);
        menu.add(item);
    }

    private void configureMenuItem(JMenuItem item, Action action) {
        String name = stringValue(action, Action.NAME);
        String description = stringValue(action, Action.SHORT_DESCRIPTION);
        String id = stringValue(action, Action.ACTION_COMMAND_KEY);
        item.setName("menu." + id);
        item.getAccessibleContext().setAccessibleName(name);
        item.getAccessibleContext().setAccessibleDescription(description);
    }

    private JMenu menu(String text, int mnemonic, String id) {
        JMenu menu = new JMenu(text);
        menu.setMnemonic(mnemonic);
        menu.setName("menu." + id);
        menu.getAccessibleContext().setAccessibleName(text);
        menu.getAccessibleContext().setAccessibleDescription(text + " menu");
        return menu;
    }

    private String stringValue(Action action, String key) {
        Object value = action.getValue(key);
        return value == null ? "" : value.toString();
    }
}
