package org.dustyroom.ui.loading;

public interface ViewerView {
    default void showPages(java.util.List<org.dustyroom.be.models.PageRef> pages) {
    }

    default void showRecentBooks(java.util.List<RecentBook> books) {
    }

    default void showAnimations(org.dustyroom.be.models.PageKey page, java.util.List<AnimatedPage> animations) {
    }

    void showLoading();

    void showFrame(ViewerFrame frame);

    void showError(String message);

    default void showPersistenceWarning(String message) {
    }
}
