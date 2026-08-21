package org.dustyroom.ui.actions;

import javax.swing.*;

public interface ViewerActions {
    Action openFile();

    Action exit();

    Action escapeFullscreen();

    Action fitHeight();

    Action fitWidth();

    Action fitScreen();

    Action zoomIn();

    Action zoomOut();

    Action nextImage();

    Action prevImage();

    Action firstImage();

    Action lastImage();

    Action nextVolume();

    Action prevVolume();

    Action toggleFullscreen();

    Action toggleTwoPageMode();

    Action setComicsReadingMode();

    Action setMangaReadingMode();

    Action setDarkTheme();

    Action setLightTheme();

    Action showAbout();
}
