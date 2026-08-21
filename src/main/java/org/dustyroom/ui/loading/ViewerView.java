package org.dustyroom.ui.loading;

public interface ViewerView {
    void showLoading();

    void showFrame(ViewerFrame frame);

    void showError(String message);
}
