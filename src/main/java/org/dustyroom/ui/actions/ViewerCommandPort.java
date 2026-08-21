package org.dustyroom.ui.actions;

/** Commands exposed to Swing actions without coupling them to controller construction. */
public interface ViewerCommandPort {
    void chooseFile();

    void exit();

    void escapeFullscreen();

    void fitHeight();

    void fitWidth();

    void fitScreen();

    void zoomIn();

    void zoomOut();

    void showNextImage();

    void showPreviousImage();

    void showFirstImage();

    void showLastImage();

    void showNextVolume();

    void showPrevVolume();

    void toggleFullscreen();

    void toggleTwoPageMode();

    void setComicsReadingMode();

    void setMangaReadingMode();

    boolean setDarkTheme();

    boolean setLightTheme();

    void showAboutDialog();
}
