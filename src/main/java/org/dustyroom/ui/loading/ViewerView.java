package org.dustyroom.ui.loading;

import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.be.models.view.RecentBook;
import org.dustyroom.be.models.view.ViewerFrame;

import java.util.List;

public interface ViewerView {
    default void showPages(List<PageRef> pages) {
    }

    default void showRecentBooks(List<RecentBook> books) {
    }

    default void showAnimations(PageKey page, List<AnimatedPage> animations) {
    }

    default void useDefaultScale(FitMode mode) {
    }

    void showLoading();

    void showFrame(ViewerFrame frame);

    void showError(String message);

    default void showPersistenceWarning(String message) {
    }
}
