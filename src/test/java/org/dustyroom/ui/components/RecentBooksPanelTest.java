package org.dustyroom.ui.components;

import org.dustyroom.be.models.direction.ReadingMode;
import org.dustyroom.be.models.state.BookState;
import org.dustyroom.be.models.view.RecentBook;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecentBooksPanelTest {
    @Test
    void missingBooksCanBeRemovedAndCoverUpdatesKeepExistingButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecentBooksPanel panel = new RecentBooksPanel();
            List<String> opened = new ArrayList<>(), removed = new ArrayList<>();
            panel.setHandlers(opened::add, removed::add);
            BookState state = new BookState("/books/volume.zip", "p2", 2, 10, false, ReadingMode.MANGA);
            panel.showBooks(List.of(new RecentBook(state, false, null)));
            JButton title = buttons(panel)
                    .stream()
                    .filter(b -> b.getText().equals("volume.zip"))
                    .findFirst()
                    .orElseThrow();
            JButton remove = buttons(panel)
                    .stream()
                    .filter(b -> b.getText().equals("Remove from recent"))
                    .findFirst()
                    .orElseThrow();
            assertFalse(title.isEnabled());
            title.doClick();
            assertTrue(opened.isEmpty());
            remove.doClick();
            assertEquals(List.of(state.getSourceId()), removed);
            panel.showBooks(List.of(new RecentBook(state, true, new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB))));
            assertTrue(buttons(panel).contains(title));
            assertTrue(title.isEnabled());
            title.doClick();
            assertEquals(List.of(state.getSourceId()), opened);
            panel.showBooks(List.of());
            assertTrue(buttons(panel).isEmpty());
        });
    }

    private List<JButton> buttons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button) result.add(button);
            if (child instanceof Container container) result.addAll(buttons(container));
        }
        return result;
    }
}
