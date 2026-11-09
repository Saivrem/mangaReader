package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.theme.ViewerTheme;
import org.dustyroom.ui.utils.UiUtils;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.awt.event.KeyEvent;

public class MenuBar extends JMenuBar {
    public MenuBar(ViewerActions actions, Predicate<ViewerTheme> selectTheme) {
        JMenu fileMenu = menu("File", KeyEvent.VK_F);
        JMenu viewMenu = menu("View", KeyEvent.VK_V);
        JMenu navigationMenu = menu("Navigation", KeyEvent.VK_N);
        JMenu optionsMenu = menu("Options", KeyEvent.VK_O);
        JMenu helpMenu = menu("Help", KeyEvent.VK_H);
        JMenu readingModeMenu = menu("Reading Mode", KeyEvent.VK_R);

        addActions(fileMenu, actions.openFile(), actions.exit());

        addActions(viewMenu,
                actions.fitHeight(), actions.fitWidth(), actions.fitScreen(),
                actions.zoomIn(), actions.zoomOut());

        addActions(navigationMenu,
                actions.nextImage(), actions.prevImage(), actions.firstImage(), actions.lastImage(),
                actions.prevVolume(), actions.nextVolume());

        addActions(readingModeMenu,
                actions.setComicsReadingMode(), actions.setMangaReadingMode(), actions.toggleTwoPageMode());

        addActions(optionsMenu, actions.toggleFullscreen());
        addActions(helpMenu, actions.showAbout());

        optionsMenu.add(readingModeMenu);
        optionsMenu.add(themeMenu(selectTheme));

        add(fileMenu);
        add(viewMenu);
        add(navigationMenu);
        add(optionsMenu);
        add(helpMenu);
    }

    private static JMenu themeMenu(Predicate<ViewerTheme> selectTheme) {
        JMenu menu = menu("Theme", KeyEvent.VK_T);
        ButtonGroup group = new ButtonGroup();
        Map<ViewerTheme, JRadioButtonMenuItem> items = new EnumMap<>(ViewerTheme.class);
        ViewerTheme[] selected = {UiUtils.currentTheme()};
        for (ViewerTheme theme : ViewerTheme.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.label());
            item.setSelected(theme == UiUtils.currentTheme());
            item.addActionListener(event -> {
                if (selectTheme.test(theme)) selected[0] = theme;
                // Also restore the previous selection if installation failed.
                items.get(selected[0]).setSelected(true);
            });
            group.add(item);
            items.put(theme, item);
            menu.add(item);
        }
        return menu;
    }

    private static JMenu menu(String name, int mnemonic) {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private static void addActions(JMenu menu, Action... actions) {
        for (Action action : actions) {
            menu.add(action);
        }
    }
}
