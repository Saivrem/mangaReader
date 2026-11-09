package org.dustyroom.ui.actions;

import org.dustyroom.ui.PlatformUi;
import org.dustyroom.ui.ViewerController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class DefaultViewerActions implements ViewerActions {
    private final Action toggleThumbnails;
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
    private final Action resetApplication;
    private final Action goToPage;
    private final Action exitFullscreen;

    public DefaultViewerActions(ViewerController controller) {
        this.toggleThumbnails = action("toggleThumbnails", "Thumbnails", "Show or hide page thumbnails (T)",
                key(KeyEvent.VK_T), controller::toggleThumbnails);
        this.openFile = action("openFile", "Open…", "Open an image, ZIP archive or folder",
                menuShortcut(KeyEvent.VK_O), controller::chooseFile);
        this.exit = action("exit", PlatformUi.usesScreenMenuBar() ? "Quit" : "Exit", "Close Yonaka Reader",
                menuShortcut(KeyEvent.VK_Q), controller::exit);
        this.fitHeight = action("fitHeight", "Fit Height", "Fit the page to the window height",
                key(KeyEvent.VK_H), controller::fitHeight);
        this.fitWidth = action("fitWidth", "Fit Width", "Fit the page to the window width",
                key(KeyEvent.VK_W), controller::fitWidth);
        this.fitScreen = action("fitScreen", "Fit to Window", "Fit the entire page inside the window",
                key(KeyEvent.VK_S), controller::fitScreen);
        this.zoomIn = action("zoomIn", "Zoom In", "Increase the page scale",
                key(KeyEvent.VK_PLUS), controller::zoomIn);
        this.zoomOut = action("zoomOut", "Zoom Out", "Decrease the page scale",
                key(KeyEvent.VK_MINUS), controller::zoomOut);
        this.nextImage = action("nextImage", "Next Page", "Show the next page",
                key(KeyEvent.VK_RIGHT), controller::showNextImage);
        this.prevImage = action("previousImage", "Previous Page", "Show the previous page",
                key(KeyEvent.VK_LEFT), controller::showPreviousImage);
        this.firstImage = action("firstImage", "First Page", "Jump to the first page",
                key(KeyEvent.VK_HOME), controller::showFirstImage);
        this.lastImage = action("lastImage", "Last Page", "Jump to the last page",
                key(KeyEvent.VK_END), controller::showLastImage);
        this.nextVolume = action("nextVolume", "Next Volume", "Open the next volume",
                key(KeyEvent.VK_DOWN), controller::showNextVolume);
        this.prevVolume = action("previousVolume", "Previous Volume", "Open the previous volume",
                key(KeyEvent.VK_UP), controller::showPrevVolume);
        this.toggleFullscreen = action("toggleFullscreen", "Full Screen", "Enter or leave full-screen mode",
                key(KeyEvent.VK_F), controller::toggleFullscreen);
        this.toggleTwoPageMode = action("toggleTwoPageMode", "Two-Page Spread", "Show compatible pages as a spread",
                key(KeyEvent.VK_P), controller::toggleTwoPageMode);
        this.setComicsReadingMode = action("comicsReadingMode", "Comics (Left to Right)", "Use left-to-right spread order",
                key(KeyEvent.VK_C), controller::setComicsReadingMode);
        this.setMangaReadingMode = action("mangaReadingMode", "Manga (Right to Left)", "Use right-to-left spread order",
                key(KeyEvent.VK_M), controller::setMangaReadingMode);
        this.goToPage = action("goToPage", "Go to Page…", "Jump to a page (G)",
                key(KeyEvent.VK_G), controller::choosePage);
        this.exitFullscreen = action("exitFullscreen", "Leave Full Screen", "Leave full-screen mode",
                key(KeyEvent.VK_ESCAPE), controller::exitFullscreen);
        this.resetApplication = action("resetApplication", "Reset Application…",
                "Reset settings and reading history", null, controller::resetApplication);
        this.showAbout = action("showAbout", "About Yonaka Reader", "Show application information",
                null, controller::showAboutDialog);
    }

    static Action action(String id, String name, String description,
                         KeyStroke accelerator, Runnable runnable) {
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, id);
        action.putValue(Action.SHORT_DESCRIPTION, description);
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

    @Override
    public Action toggleThumbnails() {
        return toggleThumbnails;
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
    public Action goToPage() {
        return goToPage;
    }

    @Override
    public Action exitFullscreen() {
        return exitFullscreen;
    }

    @Override
    public Action resetApplication() {
        return resetApplication;
    }

    @Override
    public Action showAbout() {
        return showAbout;
    }
}
