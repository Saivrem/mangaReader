package org.dustyroom.ui.navigation;

import org.dustyroom.be.models.Picture;

import java.awt.image.BufferedImage;
import java.util.List;

public class SpreadPlanner {

    public Picture resolveSecondPage(Picture currentPage, Picture nextPage, boolean twoPageMode) {
        if (!twoPageMode || currentPage == null || nextPage == null) {
            return null;
        }
        if (isLandscape(currentPage.image()) || isLandscape(nextPage.image())) {
            return null;
        }
        return nextPage;
    }

    public int resolveStep(Picture currentPage, Picture secondPage) {
        if (currentPage == null) return 0;
        return secondPage == null ? 1 : 2;
    }

    public int findPreviousAnchorIndex(List<Picture> pageHistory, int currentAnchorIndex, boolean twoPageMode) {
        for (int candidate = currentAnchorIndex - 1; candidate >= 0; candidate--) {
            Picture current = pageHistory.get(candidate);
            Picture next = candidate + 1 < pageHistory.size() ? pageHistory.get(candidate + 1) : null;
            Picture second = resolveSecondPage(current, next, twoPageMode);
            int step = resolveStep(current, second);
            if (candidate + step == currentAnchorIndex) {
                return candidate;
            }
        }
        return currentAnchorIndex - 1;
    }

    private boolean isLandscape(BufferedImage image) {
        return image != null && image.getWidth() > image.getHeight();
    }
}
