package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.theme.ViewerTheme;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

public class MenuBar extends JMenuBar {
    private final JMenu recentMenu = menu("Open Recent", KeyEvent.VK_R);
    private final JMenuItem home = new JMenuItem("Home", KeyEvent.VK_H);
    private java.util.function.Consumer<String> openRecent = id -> {
    };
    private java.util.List<org.dustyroom.ui.loading.RecentBook> recentBooks = java.util.List.of();

    public MenuBar(ViewerActions actions, Predicate<ViewerTheme> selectTheme) {
        JMenu fileMenu = menu("File", KeyEvent.VK_F);
        JMenu viewMenu = menu("View", KeyEvent.VK_V);
        JMenu navigationMenu = menu("Navigation", KeyEvent.VK_N);
        JMenu optionsMenu = menu("Options", KeyEvent.VK_O);
        JMenu helpMenu = menu("Help", KeyEvent.VK_H);
        JMenu readingModeMenu = menu("Reading Mode", KeyEvent.VK_R);

        addActions(fileMenu, actions.openFile());
        fileMenu.add(recentMenu);
        fileMenu.add(home);
        fileMenu.addSeparator();
        addActions(fileMenu, actions.exit());
        showRecentBooks(java.util.List.of());

        viewMenu.add(item(new JCheckBoxMenuItem(actions.toggleThumbnails())));
        viewMenu.addSeparator();
        addRadioActions(viewMenu, actions.fitHeight(), actions.fitWidth(), actions.fitScreen());
        viewMenu.addSeparator();
        addActions(viewMenu, actions.zoomIn(), actions.zoomOut());

        addActions(navigationMenu,
                actions.nextImage(), actions.prevImage(), actions.firstImage(), actions.lastImage(),
                actions.prevVolume(), actions.nextVolume(), actions.goToPage());

        addRadioActions(readingModeMenu, actions.setComicsReadingMode(), actions.setMangaReadingMode());
        readingModeMenu.add(item(new JCheckBoxMenuItem(actions.toggleTwoPageMode())));

        optionsMenu.add(item(new JCheckBoxMenuItem(actions.toggleFullscreen())));
        addActions(helpMenu, actions.showAbout());

        optionsMenu.add(readingModeMenu);
        optionsMenu.add(themeMenu(selectTheme));
        optionsMenu.addSeparator();
        addActions(optionsMenu, actions.resetApplication());

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

    private static void addRadioActions(JMenu menu, Action... actions) {
        ButtonGroup group = new ButtonGroup();
        for (Action action : actions) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
            group.add(item);
            menu.add(item(item));
        }
    }

    private static JMenuItem item(JMenuItem item) {
        Object command = item.getAction().getValue(Action.ACTION_COMMAND_KEY);
        if (command instanceof String id) {
            int key = switch (id) {
                case "openFile", "zoomOut" -> KeyEvent.VK_O;
                case "exit" -> KeyEvent.VK_Q;
                case "fitHeight" -> KeyEvent.VK_H;
                case "fitWidth" -> KeyEvent.VK_W;
                case "fitScreen" -> KeyEvent.VK_S;
                case "zoomIn" -> KeyEvent.VK_I;
                case "nextImage" -> KeyEvent.VK_N;
                case "previousImage" -> KeyEvent.VK_P;
                case "firstImage", "toggleFullscreen" -> KeyEvent.VK_F;
                case "lastImage" -> KeyEvent.VK_L;
                case "nextVolume" -> KeyEvent.VK_X;
                case "previousVolume" -> KeyEvent.VK_V;
                case "toggleTwoPageMode", "toggleThumbnails" -> KeyEvent.VK_T;
                case "comicsReadingMode" -> KeyEvent.VK_C;
                case "mangaReadingMode" -> KeyEvent.VK_M;
                case "goToPage" -> KeyEvent.VK_G;
                case "showAbout" -> KeyEvent.VK_A;
                default -> 0;
            };
            item.setMnemonic(key);
        }
        return item;
    }

    private static void addActions(JMenu menu, Action... actions) {
        for (Action action : actions) {
            menu.add(item(new JMenuItem(action)));
        }
    }

    public void setRecentHandlers(java.util.function.Consumer<String> open, Runnable refresh, Runnable showHome) {
        openRecent = open;
        home.addActionListener(event -> showHome.run());
        recentMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuSelected(javax.swing.event.MenuEvent event) {
                refresh.run();
            }

            public void menuDeselected(javax.swing.event.MenuEvent event) {
            }

            public void menuCanceled(javax.swing.event.MenuEvent event) {
            }
        });
    }

    public void showRecentBooks(java.util.List<org.dustyroom.ui.loading.RecentBook> books) {
        boolean unchanged = books.size() == recentBooks.size();
        for (int i = 0; unchanged && i < books.size(); i++) {
            unchanged = books.get(i).state().equals(recentBooks.get(i).state())
                    && books.get(i).available() == recentBooks.get(i).available();
        }
        if (unchanged && recentMenu.getItemCount() > 0) return;
        recentBooks = java.util.List.copyOf(books);
        recentMenu.removeAll();
        if (books.isEmpty()) {
            JMenuItem empty = new JMenuItem("No recently read books");
            empty.setEnabled(false);
            recentMenu.add(empty);
        }
        for (var book : books) {
            JMenuItem item = new JMenuItem();
            item.putClientProperty("html.disable", true);
            item.setText(book.title() + " — " + book.progress() + (book.available() ? "" : " (unavailable)"));
            item.setToolTipText(book.state().sourceId());
            item.setEnabled(book.available());
            item.addActionListener(event -> openRecent.accept(book.state().sourceId()));
            recentMenu.add(item);
        }
    }
}
