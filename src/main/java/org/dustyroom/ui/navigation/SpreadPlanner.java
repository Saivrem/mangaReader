package org.dustyroom.ui.navigation;

import org.dustyroom.be.models.PageShape;

public class SpreadPlanner {

    public boolean shouldPair(PageShape currentPage, PageShape nextPage, boolean twoPageMode) {
        return twoPageMode
                && currentPage != null
                && nextPage != null
                && !currentPage.isLandscape()
                && !nextPage.isLandscape();
    }

}
