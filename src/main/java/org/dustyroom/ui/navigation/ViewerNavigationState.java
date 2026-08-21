package org.dustyroom.ui.navigation;

import lombok.Getter;
import lombok.Setter;
import org.dustyroom.be.models.Picture;

import java.util.ArrayList;
import java.util.List;

public class ViewerNavigationState {
    private final List<Picture> pageHistory = new ArrayList<>();
    @Setter
    @Getter
    private int pageIndex = -1;

    public List<Picture> pageHistory() {
        return pageHistory;
    }

    public int pageIndex() {
        return pageIndex;
    }
}
