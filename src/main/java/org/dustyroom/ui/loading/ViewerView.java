package org.dustyroom.ui.loading;

public interface ViewerView {
    default void showPages(java.util.List<org.dustyroom.be.models.PageRef> pages) {}

    void showLoading();

    void showFrame(ViewerFrame frame);

    void showError(String message);

    default void showPersistenceWarning(String message) {}
}
