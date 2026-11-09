package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.navigation.ReadingMode;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class NavigationPanel extends JPanel {
    private final JButton position;
    private final JButton fit = new JButton("Fit to window ▾");
    private final JButton direction = new JButton("Manga ▾");
    private String pageLabel = "No book open";

    public NavigationPanel(ViewerActions actions) {
        setLayout(new BorderLayout(12, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel files = group();
        files.add(button(actions.openFile(), ReaderIcon.Kind.OPEN, "Open"));
        files.add(button(actions.prevVolume(), ReaderIcon.Kind.PREVIOUS_VOLUME, null));
        files.add(button(actions.nextVolume(), ReaderIcon.Kind.NEXT_VOLUME, null));
        add(files, BorderLayout.WEST);

        JPanel pages = group();
        pages.add(button(actions.prevImage(), ReaderIcon.Kind.PREVIOUS, null));
        position = new JButton(actions.goToPage());
        position.setText(pageLabel);
        position.setPreferredSize(new Dimension(138, 36));
        position.setToolTipText("Go to page (G)");
        position.getAccessibleContext().setAccessibleName("Page position; activate to go to a page");
        pages.add(position);
        pages.add(button(actions.nextImage(), ReaderIcon.Kind.NEXT, null));
        add(pages, BorderLayout.CENTER);

        JPanel settings = group();
        JPopupMenu fitMenu = new JPopupMenu();
        radioItems(fitMenu, actions.fitScreen(), actions.fitWidth(), actions.fitHeight());
        fitMenu.addSeparator();
        fitMenu.add(actions.zoomIn());
        fitMenu.add(actions.zoomOut());
        fit.addActionListener(event -> {
            javax.swing.SwingUtilities.updateComponentTreeUI(fitMenu);
            fitMenu.show(fit, 0, fit.getHeight());
        });
        fit.setToolTipText("Page scale: S window, W width, H height");
        settings.add(fit);
        JToggleButton spread = new JToggleButton(actions.toggleTwoPageMode());
        decorate(spread, ReaderIcon.Kind.SPREAD, null);
        settings.add(spread);
        JPopupMenu readingMenu = new JPopupMenu();
        radioItems(readingMenu, actions.setMangaReadingMode(), actions.setComicsReadingMode());
        direction.addActionListener(event -> {
            javax.swing.SwingUtilities.updateComponentTreeUI(readingMenu);
            readingMenu.show(direction, 0, direction.getHeight());
        });
        direction.setToolTipText("Reading direction: M manga, C comics");
        settings.add(direction);
        JToggleButton fullscreen = new JToggleButton(actions.toggleFullscreen());
        decorate(fullscreen, ReaderIcon.Kind.FULLSCREEN, null);
        settings.add(fullscreen);
        add(settings, BorderLayout.EAST);
        fit.setEnabled(false);
        direction.setEnabled(false);
    }

    public void updateState(ViewerFrame frame, ImageComponent.FitMode mode) {
        if (frame != null) {
            pageLabel = (frame.firstPage() == frame.lastPage() ? String.valueOf(frame.firstPage())
                    : frame.firstPage() + "–" + frame.lastPage()) + " / " + frame.pageCount();
            direction.setText(frame.readingMode() == ReadingMode.MANGA ? "Manga ▾" : "Comics ▾");
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

    private static JPanel group() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        panel.setOpaque(false);
        return panel;
    }

    private static JButton button(Action action, ReaderIcon.Kind icon, String text) {
        JButton button = new JButton(action);
        decorate(button, icon, text);
        return button;
    }

    private static void decorate(AbstractButton button, ReaderIcon.Kind icon, String text) {
        button.setText(text);
        Object shortcut = button.getAction().getValue(Action.ACCELERATOR_KEY);
        String description = (String) button.getAction().getValue(Action.SHORT_DESCRIPTION);
        if (shortcut instanceof javax.swing.KeyStroke key) {
            String modifiers = java.awt.event.InputEvent.getModifiersExText(key.getModifiers());
            button.setToolTipText(description + " (" + (modifiers.isEmpty() ? "" : modifiers + "+")
                    + java.awt.event.KeyEvent.getKeyText(key.getKeyCode()) + ")");
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
}
