package org.dustyroom.ui.loading;

import org.dustyroom.be.models.PageKey;
import org.dustyroom.be.models.PageRef;

import java.util.List;

public interface ViewerView {
    default void showPages(List<PageRef> pages) {
    }

    default void showRecentBooks(List<RecentBook> books) {
    }

    default void showAnimations(PageKey page, List<AnimatedPage> animations) {
    }

    void showLoading();

    void showFrame(ViewerFrame frame);

    void showError(String message);

    default void showPersistenceWarning(String message) {
    }
}
