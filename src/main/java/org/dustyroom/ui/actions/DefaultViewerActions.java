package org.dustyroom.ui.actions;

import org.dustyroom.ui.ViewerController;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
    private final Action setNimbusTheme;
    private final Action setMetalTheme;
    private final Action setSystemTheme;
    private final Action showAbout;

    public DefaultViewerActions(ViewerController controller) {
        this.openFile = action(controller::chooseFile);
        this.exit = action(controller::exit);
        this.fitHeight = action(controller::fitHeight);
        this.fitWidth = action(controller::fitWidth);
        this.fitScreen = action(controller::fitScreen);
        this.zoomIn = action(controller::zoomIn);
        this.zoomOut = action(controller::zoomOut);
        this.nextImage = action(controller::showNextImage);
        this.prevImage = action(controller::showPreviousImage);
        this.firstImage = action(controller::showFirstImage);
        this.lastImage = action(controller::showLastImage);
        this.nextVolume = action(controller::showNextVolume);
        this.prevVolume = action(controller::showPrevVolume);
        this.toggleFullscreen = action(controller::toggleFullscreen);
        this.toggleTwoPageMode = action(controller::toggleTwoPageMode);
        this.setComicsReadingMode = action(controller::setComicsReadingMode);
        this.setMangaReadingMode = action(controller::setMangaReadingMode);
        this.setNimbusTheme = action(controller::setNimbusTheme);
        this.setMetalTheme = action(controller::setMetalTheme);
        this.setSystemTheme = action(controller::setSystemTheme);
        this.showAbout = action(controller::showAboutDialog);
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
    public Action setNimbusTheme() {
        return setNimbusTheme;
    }

    @Override
    public Action setMetalTheme() {
        return setMetalTheme;
    }

    @Override
    public Action setSystemTheme() {
        return setSystemTheme;
    }

    @Override
    public Action showAbout() {
        return showAbout;
    }

    private static Action action(Runnable runnable) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runnable.run();
            }
        };
    }
}
