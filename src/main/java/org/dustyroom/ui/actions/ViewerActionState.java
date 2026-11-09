package org.dustyroom.ui.actions;

import org.dustyroom.ui.components.ImageComponent.FitMode;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.navigation.ReadingMode;

import javax.swing.*;
import java.util.List;

/**
 * Publishes the displayed state to shared menu, toolbar and keyboard actions.
 */
public final class ViewerActionState {
    private ViewerActionState() {
    }

    public static void apply(ViewerActions actions, ViewerFrame frame, FitMode fit, boolean fullscreen) {
        boolean loaded = frame != null;
        for (Action action : List.of(actions.nextImage(), actions.prevImage(), actions.firstImage(),
                actions.lastImage(), actions.nextVolume(), actions.prevVolume(), actions.goToPage(),
                actions.fitHeight(), actions.fitWidth(), actions.fitScreen(), actions.zoomIn(), actions.zoomOut(),
                actions.toggleTwoPageMode(), actions.setMangaReadingMode(), actions.setComicsReadingMode())) {
            action.setEnabled(loaded);
        }
        // Next/previous intentionally retain the reader's existing wrap-around behavior.
        actions.firstImage().setEnabled(loaded && frame.firstPage() > 1);
        actions.lastImage().setEnabled(loaded && frame.lastPage() < frame.pageCount());
        actions.exitFullscreen().setEnabled(fullscreen);
        selected(actions.fitHeight(), fit == FitMode.FIT_HEIGHT);
        selected(actions.fitWidth(), fit == FitMode.FIT_WIDTH);
        selected(actions.fitScreen(), fit == FitMode.FIT_SCREEN);
        selected(actions.toggleTwoPageMode(), loaded && frame.twoPageMode());
        selected(actions.setMangaReadingMode(), !loaded || frame.readingMode() == ReadingMode.MANGA);
        selected(actions.setComicsReadingMode(), loaded && frame.readingMode() == ReadingMode.COMICS);
        selected(actions.toggleFullscreen(), fullscreen);
    }

    private static void selected(Action action, boolean selected) {
        action.putValue(Action.SELECTED_KEY, selected);
    }
}
