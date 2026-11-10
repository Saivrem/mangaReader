package org.dustyroom.ui.components;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.listeners.MenuOpenListener;
import org.dustyroom.ui.utils.UiUtils;

public class MenuBar extends JMenuBar {
    private final JMenu recentMenu = menu("Open Recent", KeyEvent.VK_R);
    private final JMenuItem home;
    private final Map<ViewerTheme, JRadioButtonMenuItem> themeItems = new EnumMap<>(ViewerTheme.class);
    private ViewerTheme selectedTheme = UiUtils.currentTheme();
    private Consumer<String> openRecent = id -> {
    };
    private List<RecentBook> recentBooks = List.of();

    public MenuBar(ViewerActions actions, Predicate<ViewerTheme> selectTheme) {
        home = new JMenuItem(actions.home());
        home.setMnemonic(KeyEvent.VK_H);
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
        showRecentBooks(List.of());

        viewMenu.add(item(new JCheckBoxMenuItem(actions.toggleThumbnails())));
        JMenuItem grid = item(new JMenuItem(actions.openPageGrid()));
        grid.setIcon(new ReaderIcon(ReaderIconKind.PAGE_GRID, 16));
        viewMenu.add(grid);
        viewMenu.addSeparator();
        addRadioActions(viewMenu, actions.fitHeight(), actions.fitWidth(), actions.fitScreen());
        viewMenu.addSeparator();
        addActions(viewMenu, actions.zoomIn(), actions.zoomOut());

        addActions(navigationMenu,
                actions.nextImage(), actions.prevImage(), actions.firstImage(), actions.lastImage(),
                actions.prevVolume(), actions.nextVolume(), actions.goToPage());

        addRadioActions(readingModeMenu, actions.setComicsReadingMode(), actions.setMangaReadingMode());
        readingModeMenu.add(item(new JCheckBoxMenuItem(actions.toggleTwoPageMode())));

        addActions(optionsMenu, actions.settings());
        optionsMenu.addSeparator();
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

    private JMenu themeMenu(Predicate<ViewerTheme> selectTheme) {
        JMenu menu = menu("Theme", KeyEvent.VK_T);
        ButtonGroup group = new ButtonGroup();
        for (ViewerTheme theme : ViewerTheme.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.label());
            item.setSelected(theme == UiUtils.currentTheme());
            item.addActionListener(event -> {
                if (selectTheme.test(theme)) {
                    selectedTheme = theme;
                }
                // Also restore the previous selection if installation failed.
                showTheme(selectedTheme);
            });
            group.add(item);
            themeItems.put(theme, item);
            menu.add(item);
        }
        return menu;
    }

    public void showTheme(ViewerTheme theme) {
        selectedTheme = theme;
        themeItems.get(theme).setSelected(true);
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
                case "goToPage", "openPageGrid" -> KeyEvent.VK_G;
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

    public void setRecentHandlers(Consumer<String> open, Runnable refresh) {
        openRecent = open;

        recentMenu.addMenuListener(new MenuOpenListener(refresh));
    }

    public void showRecentBooks(List<RecentBook> books) {
        boolean unchanged = books.size() == recentBooks.size();
        for (int i = 0; unchanged && i < books.size(); i++) {
            unchanged = books.get(i).getState().equals(recentBooks.get(i).getState())
                    && books.get(i).isAvailable() == recentBooks.get(i).isAvailable()
                    && books.get(i).getChapters().equals(recentBooks.get(i).getChapters());
        }
        if (unchanged && recentMenu.getItemCount() > 0) {
            return;
        }
        recentBooks = List.copyOf(books);
        recentMenu.removeAll();
        if (books.isEmpty()) {
            JMenuItem empty = new JMenuItem("No recently read books");
            empty.setEnabled(false);
            recentMenu.add(empty);
        }
        for (RecentBook book : books) {
            JMenuItem item = new JMenuItem();
            item.putClientProperty("html.disable", true);
            item.setText(book.getTitle() + " — " + book.getProgress() + (book.isAvailable() ? "" : " (unavailable)"));
            item.setToolTipText(book.getState().getSourceId());
            item.setEnabled(book.isAvailable());
            item.addActionListener(event -> openRecent.accept(book.getState().getSourceId()));
            if (book.getChapters().size() > 1) {
                JMenu group = new JMenu();
                group.putClientProperty("html.disable", true);
                group.setText(book.getTitle() + " (" + book.getChapters().size() + " chapters)");
                item.setText("Continue — " + new File(book.getState().getSourceId()).getName());
                group.add(item);
                group.addSeparator();
                for (BookState chapter : book.getChapters().stream().limit(20).toList()) {
                    JMenuItem entry = new JMenuItem();
                    entry.putClientProperty("html.disable", true);
                    entry.setText(new File(chapter.getSourceId()).getName());
                    entry.setToolTipText(chapter.getSourceId());
                    entry.addActionListener(event -> openRecent.accept(chapter.getSourceId()));
                    group.add(entry);
                }
                recentMenu.add(group);
            } else {
                recentMenu.add(item);
            }
        }
    }
}
