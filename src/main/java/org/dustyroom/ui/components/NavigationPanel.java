package org.dustyroom.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.ui.actions.ViewerActions;

public class NavigationPanel extends JPanel {
    private final JButton position;
    private final JButton fit = new JButton("Fit to window ▾");
    private final JButton direction = new JButton("Manga ▾");
    private String pageLabel = "No book open";

    public NavigationPanel(ViewerActions actions) {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel files = group();
        files.add(button(actions.home(), ReaderIconKind.HOME, null));
        files.add(readerMenu(actions));
        files.add(button(actions.prevVolume(), ReaderIconKind.PREVIOUS_VOLUME, null));
        files.add(button(actions.nextVolume(), ReaderIconKind.NEXT_VOLUME, null));
        add(files, BorderLayout.WEST);

        JPanel pages = group();
        pages.add(button(actions.firstImage(), ReaderIconKind.FIRST, null));
        pages.add(button(actions.prevImage(), ReaderIconKind.PREVIOUS, null));
        position = new JButton(actions.goToPage());
        position.setText(pageLabel);
        position.setPreferredSize(new Dimension(116, 36));
        position.setToolTipText("Go to page (G)");
        position.getAccessibleContext().setAccessibleName("Page position; activate to go to a page");
        pages.add(position);
        pages.add(button(actions.nextImage(), ReaderIconKind.NEXT, null));
        pages.add(button(actions.lastImage(), ReaderIconKind.LAST, null));
        add(pages, BorderLayout.CENTER);

        JPanel settings = group();
        JPopupMenu fitMenu = new JPopupMenu();
        radioItems(fitMenu, actions.fitScreen(), actions.fitWidth(), actions.fitHeight());
        fitMenu.addSeparator();
        fitMenu.add(actions.zoomIn());
        fitMenu.add(actions.zoomOut());
        fit.addActionListener(event -> {
            SwingUtilities.updateComponentTreeUI(fitMenu);
            fitMenu.show(fit, 0, fit.getHeight());
        });
        fit.setToolTipText("Page scale: S window, W width, H height");
        settings.add(fit);
        JPopupMenu readingMenu = new JPopupMenu();
        radioItems(readingMenu, actions.setMangaReadingMode(), actions.setComicsReadingMode());
        direction.addActionListener(event -> {
            SwingUtilities.updateComponentTreeUI(readingMenu);
            readingMenu.show(direction, 0, direction.getHeight());
        });
        direction.setToolTipText("Reading direction: M manga, C comics");
        settings.add(direction);
        JToggleButton spread = new JToggleButton(actions.toggleTwoPageMode());
        decorate(spread, ReaderIconKind.SPREAD, null);
        settings.add(spread);
        add(settings, BorderLayout.EAST);
        fit.setEnabled(false);
        direction.setEnabled(false);
    }

    private static JButton readerMenu(ViewerActions actions) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(actions.openFile()).setIcon(new ReaderIcon(ReaderIconKind.OPEN, 16));
        menu.add(actions.openPageGrid()).setIcon(new ReaderIcon(ReaderIconKind.PAGE_GRID, 16));
        menu.add(new JCheckBoxMenuItem(actions.toggleFullscreen()));

        JButton button = new JButton(new ReaderIcon(ReaderIconKind.MENU));
        button.setPreferredSize(new Dimension(36, 36));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setToolTipText("Reader menu");
        button.getAccessibleContext().setAccessibleName("Reader menu");
        button.addActionListener(event -> {
            SwingUtilities.updateComponentTreeUI(menu);
            menu.show(button, 0, -menu.getPreferredSize().height);
        });
        return button;
    }

    private static JPanel group() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        panel.setOpaque(false);
        return panel;
    }

    private static JButton button(Action action, ReaderIconKind icon, String text) {
        JButton button = new JButton(action);
        decorate(button, icon, text);
        return button;
    }

    private static void decorate(AbstractButton button, ReaderIconKind icon, String text) {
        button.setText(text);
        Object shortcut = button.getAction().getValue(Action.ACCELERATOR_KEY);
        String description = (String) button.getAction().getValue(Action.SHORT_DESCRIPTION);
        if (shortcut instanceof KeyStroke key) {
            String modifiers = InputEvent.getModifiersExText(key.getModifiers());
            button.setToolTipText(description + " (" + (modifiers.isEmpty() ? "" : modifiers + "+")
                    + KeyEvent.getKeyText(key.getKeyCode()) + ")");
        }
        button.setIcon(new ReaderIcon(icon));
        button.setPreferredSize(new Dimension(text == null ? 36 : 82, 36));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.getAccessibleContext().setAccessibleName((String) button.getAction().getValue(Action.NAME));
    }

    private static void radioItems(JPopupMenu menu, Action... actions) {
        ButtonGroup group = new ButtonGroup();
        for (Action action : actions) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
            group.add(item);
            menu.add(item);
        }
    }

    public void updateState(ViewerFrame frame, FitMode mode) {
        if (frame != null) {
            pageLabel = (frame.getFirstPage() == frame.getLastPage() ? String.valueOf(frame.getFirstPage())
                    : frame.getFirstPage() + "–" + frame.getLastPage()) + " / " + frame.getPageCount();
            direction.setText(frame.getReadingMode() == ReadingMode.MANGA ? "Manga ▾" : "Comics ▾");
        }
        position.setText(pageLabel);
        fit.setText(switch (mode) {
            case FIT_SCREEN -> "Fit to window ▾";
            case FIT_WIDTH -> "Fit width ▾";
            case FIT_HEIGHT -> "Fit height ▾";
            case ORIGINAL -> "Custom zoom ▾";
        });
        fit.setEnabled(frame != null);
        direction.setEnabled(frame != null);
    }

    public void setLoading(boolean loading) {
        position.setText(loading ? "Loading…" : pageLabel);
    }
}
