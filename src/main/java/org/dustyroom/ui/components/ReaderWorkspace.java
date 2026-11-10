package org.dustyroom.ui.components;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Reuses the thumbnail list and cache when switching between sidebar and page grid. */
public final class ReaderWorkspace extends JPanel {
    private final ThumbnailPanel thumbnails;
    private final JComponent reader;
    private final JComponent navigation;
    private final Consumer<Boolean> gridChanged;
    private boolean navigationWasVisible;

    public ReaderWorkspace(ThumbnailPanel thumbnails, JComponent reader, JComponent navigation,
                           Consumer<Boolean> gridChanged) {
        super(new BorderLayout());
        this.thumbnails = thumbnails;
        this.reader = reader;
        this.navigation = navigation;
        this.gridChanged = gridChanged;
        add(thumbnails, BorderLayout.WEST);
        add(reader, BorderLayout.CENTER);
        add(navigation, BorderLayout.SOUTH);
        thumbnails.setGridChanged(this::showGrid);
    }

    private void showGrid(boolean grid) {
        remove(thumbnails);
        if (grid) {
            navigationWasVisible = navigation.isVisible();
            // Keep the hidden reader in the component tree so theme changes reach it.
            reader.setVisible(false);
            navigation.setVisible(false);
            add(thumbnails, BorderLayout.CENTER);
        } else {
            add(thumbnails, BorderLayout.WEST);
            add(reader, BorderLayout.CENTER);
            reader.setVisible(true);
            navigation.setVisible(navigationWasVisible);
        }
        revalidate();
        repaint();
        gridChanged.accept(grid);
    }
}
