package org.dustyroom.ui.actions;

import org.dustyroom.ui.ViewerController;
import org.dustyroom.ui.PlatformUi;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class DefaultViewerActions implements ViewerActions {
    private final Action openFile;
    private final Action exit;
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
    private final Action showAbout;

    public DefaultViewerActions(ViewerController controller) {
        this.openFile = action("openFile", "Open…", "Open an image or ZIP archive",
                KeyEvent.VK_O, menuShortcut(KeyEvent.VK_O), controller::chooseFile);
        this.exit = action("exit", PlatformUi.usesScreenMenuBar() ? "Quit" : "Exit", "Close Manga Reader",
                KeyEvent.VK_Q, menuShortcut(KeyEvent.VK_Q), controller::exit);
        this.fitHeight = action("fitHeight", "Fit Height", "Fit the page to the window height",
                KeyEvent.VK_H, key(KeyEvent.VK_H), controller::fitHeight);
        this.fitWidth = action("fitWidth", "Fit Width", "Fit the page to the window width",
                KeyEvent.VK_W, key(KeyEvent.VK_W), controller::fitWidth);
        this.fitScreen = action("fitScreen", "Fit to Window", "Fit the entire page inside the window",
                KeyEvent.VK_S, key(KeyEvent.VK_S), controller::fitScreen);
        this.zoomIn = action("zoomIn", "Zoom In", "Increase the page scale",
                KeyEvent.VK_I, key(KeyEvent.VK_PLUS), controller::zoomIn);
        this.zoomOut = action("zoomOut", "Zoom Out", "Decrease the page scale",
                KeyEvent.VK_O, key(KeyEvent.VK_MINUS), controller::zoomOut);
        this.nextImage = action("nextImage", "Next Page", "Show the next page",
                KeyEvent.VK_N, key(KeyEvent.VK_RIGHT), controller::showNextImage);
        this.prevImage = action("previousImage", "Previous Page", "Show the previous page",
                KeyEvent.VK_P, key(KeyEvent.VK_LEFT), controller::showPreviousImage);
        this.firstImage = action("firstImage", "First Page", "Jump to the first page",
                KeyEvent.VK_F, key(KeyEvent.VK_HOME), controller::showFirstImage);
        this.lastImage = action("lastImage", "Last Page", "Jump to the last page",
                KeyEvent.VK_L, key(KeyEvent.VK_END), controller::showLastImage);
        this.nextVolume = action("nextVolume", "Next Volume", "Open the next volume",
                KeyEvent.VK_X, key(KeyEvent.VK_DOWN), controller::showNextVolume);
        this.prevVolume = action("previousVolume", "Previous Volume", "Open the previous volume",
                KeyEvent.VK_V, key(KeyEvent.VK_UP), controller::showPrevVolume);
        this.toggleFullscreen = action("toggleFullscreen", "Toggle Full Screen", "Enter or leave full-screen mode",
                KeyEvent.VK_F, key(KeyEvent.VK_F), controller::toggleFullscreen);
        this.toggleTwoPageMode = action("toggleTwoPageMode", "Toggle Two-Page Mode", "Show compatible pages as a spread",
                KeyEvent.VK_T, key(KeyEvent.VK_P), controller::toggleTwoPageMode);
        this.setComicsReadingMode = action("comicsReadingMode", "Comics (Left to Right)", "Use left-to-right spread order",
                KeyEvent.VK_C, key(KeyEvent.VK_C), controller::setComicsReadingMode);
        this.setMangaReadingMode = action("mangaReadingMode", "Manga (Right to Left)", "Use right-to-left spread order",
                KeyEvent.VK_M, key(KeyEvent.VK_M), controller::setMangaReadingMode);
        this.showAbout = action("showAbout", "About Manga Reader", "Show application information",
                KeyEvent.VK_A, null, controller::showAboutDialog);
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
    public Action showAbout() {
        return showAbout;
    }

    static Action action(String id, String name, String description, int mnemonic,
                         KeyStroke accelerator, Runnable runnable) {
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, id);
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.MNEMONIC_KEY, mnemonic);
        if (accelerator != null) {
            action.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        return action;
    }

    private static KeyStroke menuShortcut(int keyCode) {
        int modifiers;
        try {
            modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (HeadlessException exception) {
            modifiers = InputEvent.CTRL_DOWN_MASK;
        }
        return KeyStroke.getKeyStroke(keyCode, modifiers);
    }

    private static KeyStroke key(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, 0);
    }
}
