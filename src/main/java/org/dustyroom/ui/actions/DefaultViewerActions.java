package org.dustyroom.ui.actions;

import org.dustyroom.ui.LookSettings;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

public class DefaultViewerActions implements ViewerActions {
    private final ViewerCommandPort controller;
    private final Action openFile;
    private final Action exit;
    private final Action escapeFullscreen;
    private final Action fitHeight;
    private final Action fitWidth;
    private final Action fitScreen;
    private final Action zoomIn;
    private final Action zoomOut;
    private final Action nextImage;
    private final Action prevImage;
    private final Action firstImage;
    private final Action lastImage;
    private final Action nextVolume;
    private final Action prevVolume;
    private final Action toggleFullscreen;
    private final Action toggleTwoPageMode;
    private final Action setComicsReadingMode;
    private final Action setMangaReadingMode;
    private final Action setDarkTheme;
    private final Action setLightTheme;
    private final Action showAbout;
    private Action selectedThemeAction;

    public DefaultViewerActions(ViewerCommandPort controller) {
        this.controller = controller;

        int menuShortcut = menuShortcutMask();
        this.openFile = action(
                "openFile", "Open File", "Open an image or ZIP archive",
                KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcut), controller::chooseFile
        );
        this.exit = action(
                "exit", "Exit", "Close Manga Reader",
                KeyEvent.VK_X, KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuShortcut), controller::exit
        );
        this.escapeFullscreen = action(
                "escapeFullscreen", "Exit Fullscreen", "Leave fullscreen mode",
                null, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this::performEscapeFullscreen
        );
        this.fitHeight = action(
                "fitHeight", "Fit Height", "Fit the page to the window height",
                KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), controller::fitHeight
        );
        this.fitWidth = action(
                "fitWidth", "Fit Width", "Fit the page to the window width",
                KeyEvent.VK_W, KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), controller::fitWidth
        );
        this.fitScreen = action(
                "fitScreen", "Fit Screen", "Fit the whole page inside the window",
                KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), controller::fitScreen
        );
        this.zoomIn = action(
                "zoomIn", "Zoom In", "Increase the page zoom level",
                KeyEvent.VK_I, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK), controller::zoomIn
        );
        this.zoomOut = action(
                "zoomOut", "Zoom Out", "Decrease the page zoom level",
                KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), controller::zoomOut
        );
        this.nextImage = action(
                "nextImage", "Next Page", "Show the next page",
                KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), controller::showNextImage
        );
        this.prevImage = action(
                "previousImage", "Previous Page", "Show the previous page",
                KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), controller::showPreviousImage
        );
        this.firstImage = action(
                "firstImage", "First Page", "Show the first page",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), controller::showFirstImage
        );
        this.lastImage = action(
                "lastImage", "Last Page", "Show the last page",
                KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), controller::showLastImage
        );
        this.nextVolume = action(
                "nextVolume", "Next Volume", "Open the next volume",
                KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), controller::showNextVolume
        );
        this.prevVolume = action(
                "previousVolume", "Previous Volume", "Open the previous volume",
                KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), controller::showPrevVolume
        );
        this.toggleFullscreen = toggleAction(
                "toggleFullscreen", "Fullscreen", "Toggle fullscreen mode",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), false, controller::toggleFullscreen
        );
        this.toggleTwoPageMode = toggleAction(
                "toggleTwoPageMode", "Two-page Mode", "Show portrait pages as a two-page spread",
                KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), false, controller::toggleTwoPageMode
        );
        this.setComicsReadingMode = selectableAction(
                "setComicsReadingMode", "Comics (Left to Right)", "Use left-to-right spread order",
                KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), false, this::selectComicsReadingMode
        );
        this.setMangaReadingMode = selectableAction(
                "setMangaReadingMode", "Manga (Right to Left)", "Use right-to-left spread order",
                KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), true, this::selectMangaReadingMode
        );
        LookSettings currentTheme = UiUtils.getCurrent();
        if (currentTheme == null) {
            currentTheme = LookSettings.MANGA_READER_DARK;
        }
        this.setDarkTheme = selectableAction(
                "setDarkTheme", "Dark", "Use the dark color theme",
                KeyEvent.VK_D, null, currentTheme == LookSettings.MANGA_READER_DARK, this::selectDarkTheme
        );
        this.setLightTheme = selectableAction(
                "setLightTheme", "Light", "Use the light color theme",
                KeyEvent.VK_L, null, currentTheme == LookSettings.MANGA_READER_LIGHT, this::selectLightTheme
        );
        this.selectedThemeAction = switch (currentTheme) {
            case MANGA_READER_DARK -> setDarkTheme;
            case MANGA_READER_LIGHT -> setLightTheme;
        };
        this.showAbout = action(
                "showAbout", "About Manga Reader", "Show application information",
                KeyEvent.VK_A, null, controller::showAboutDialog
        );
    }

    @Override
    public Action openFile() {
        return openFile;
    }

    @Override
    public Action exit() {
        return exit;
    }

    @Override
    public Action escapeFullscreen() {
        return escapeFullscreen;
    }

    @Override
    public Action fitHeight() {
        return fitHeight;
    }

    @Override
    public Action fitWidth() {
        return fitWidth;
    }

    @Override
    public Action fitScreen() {
        return fitScreen;
    }

    @Override
    public Action zoomIn() {
        return zoomIn;
    }

    @Override
    public Action zoomOut() {
        return zoomOut;
    }

    @Override
    public Action nextImage() {
        return nextImage;
    }

    @Override
    public Action prevImage() {
        return prevImage;
    }

    @Override
    public Action firstImage() {
        return firstImage;
    }

    @Override
    public Action lastImage() {
        return lastImage;
    }

    @Override
    public Action nextVolume() {
        return nextVolume;
    }

    @Override
    public Action prevVolume() {
        return prevVolume;
    }

    @Override
    public Action toggleFullscreen() {
        return toggleFullscreen;
    }

    @Override
    public Action toggleTwoPageMode() {
        return toggleTwoPageMode;
    }

    @Override
    public Action setComicsReadingMode() {
        return setComicsReadingMode;
    }

    @Override
    public Action setMangaReadingMode() {
        return setMangaReadingMode;
    }

    @Override
    public Action setDarkTheme() {
        return setDarkTheme;
    }

    @Override
    public Action setLightTheme() {
        return setLightTheme;
    }

    @Override
    public Action showAbout() {
        return showAbout;
    }

    private void selectComicsReadingMode() {
        controller.setComicsReadingMode();
        selectOnly(setComicsReadingMode, setMangaReadingMode);
    }

    private void performEscapeFullscreen() {
        controller.escapeFullscreen();
        toggleFullscreen.putValue(Action.SELECTED_KEY, false);
    }

    private void selectMangaReadingMode() {
        controller.setMangaReadingMode();
        selectOnly(setMangaReadingMode, setComicsReadingMode);
    }

    private void selectDarkTheme() {
        applyTheme(setDarkTheme, controller::setDarkTheme);
    }

    private void selectLightTheme() {
        applyTheme(setLightTheme, controller::setLightTheme);
    }

    private void applyTheme(Action requestedTheme, BooleanSupplier installer) {
        boolean installed = false;
        try {
            installed = installer.getAsBoolean();
        } finally {
            if (installed) {
                selectedThemeAction = requestedTheme;
            }
            syncThemeSelection();
        }
    }

    private void syncThemeSelection() {
        setDarkTheme.putValue(Action.SELECTED_KEY, selectedThemeAction == setDarkTheme);
        setLightTheme.putValue(Action.SELECTED_KEY, selectedThemeAction == setLightTheme);
    }

    private static void selectOnly(Action selected, Action... deselected) {
        selected.putValue(Action.SELECTED_KEY, true);
        for (Action action : deselected) {
            action.putValue(Action.SELECTED_KEY, false);
        }
    }

    private static Action action(
            String id,
            String name,
            String description,
            Integer mnemonic,
            KeyStroke accelerator,
            Runnable runnable
    ) {
        return configure(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        }, id, description, mnemonic, accelerator);
    }

    private static Action toggleAction(
            String id,
            String name,
            String description,
            Integer mnemonic,
            KeyStroke accelerator,
            boolean selected,
            Runnable runnable
    ) {
        Action action = configure(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean buttonAlreadyUpdatedSelection = e.getSource() instanceof JToggleButton
                        || e.getSource() instanceof JCheckBoxMenuItem
                        || e.getSource() instanceof JRadioButtonMenuItem;
                boolean selectedAtDispatch = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
                try {
                    runnable.run();
                } catch (RuntimeException exception) {
                    if (buttonAlreadyUpdatedSelection) {
                        putValue(Action.SELECTED_KEY, !selectedAtDispatch);
                    }
                    throw exception;
                }
                if (!buttonAlreadyUpdatedSelection) {
                    putValue(Action.SELECTED_KEY, !selectedAtDispatch);
                }
            }
        }, id, description, mnemonic, accelerator);
        action.putValue(Action.SELECTED_KEY, selected);
        return action;
    }

    private static Action selectableAction(
            String id,
            String name,
            String description,
            Integer mnemonic,
            KeyStroke accelerator,
            boolean selected,
            Runnable runnable
    ) {
        Action action = action(id, name, description, mnemonic, accelerator, runnable);
        action.putValue(Action.SELECTED_KEY, selected);
        return action;
    }

    private static Action configure(
            Action action,
            String id,
            String description,
            Integer mnemonic,
            KeyStroke accelerator
    ) {
        action.putValue(Action.ACTION_COMMAND_KEY, id);
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.LONG_DESCRIPTION, description);
        if (mnemonic != null) {
            action.putValue(Action.MNEMONIC_KEY, mnemonic);
        }
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        return action;
    }

    private static int menuShortcutMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (HeadlessException e) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }
}
