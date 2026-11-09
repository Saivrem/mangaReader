package org.dustyroom.ui.components;

import org.dustyroom.be.models.PageKey;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.PictureMetadata;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailPanelTest {
    private static void drain(List<Consumer<BufferedImage>> replies) {
        int limit = 100;
        while (!replies.isEmpty()) {
            assertTrue(limit-- > 0, "Loading must stop at the cache window boundary");
            replies.remove(0).accept(new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB));
        }
    }

    private static List<PageRef> pages(String source) {
        return IntStream.range(0, 1000).mapToObj(i -> new PageRef(new PageKey(source, "page" + i),
                new PictureMetadata("page" + i, source, new File(source)))).toList();
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container container) layout(container);
    }

    @Test
    void loadsVisiblePagesFirstOneAtATimeAndStopsWhenHidden() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> requested = new ArrayList<>();
            List<Consumer<BufferedImage>> replies = new ArrayList<>();
            ThumbnailPanel panel = new ThumbnailPanel((index, page, result) -> {
                requested.add(index);
                replies.add(result);
            }, page -> {
            }, () -> {
            });
            panel.setSize(164, 500);
            panel.addNotify();
            try {
                layout(panel);
                panel.showPages(pages("book"));
                panel.showSpread(1, 2);
                assertEquals(List.of(0), requested);
                replies.get(0).accept(new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB));
                assertEquals(List.of(0, 1), requested);
                panel.setExpanded(false);
                replies.get(1).accept(null);
                assertEquals(2, requested.size());
                panel.showPages(pages("another"));
                assertEquals(2, requested.size());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void slidesCacheWindowAndReloadsEvictedPages() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> requested = new ArrayList<>();
            List<Consumer<BufferedImage>> replies = new ArrayList<>();
            ThumbnailPanel panel = new ThumbnailPanel((index, page, result) -> {
                requested.add(index);
                replies.add(result);
            }, page -> {
            }, () -> {
            });
            panel.setSize(164, 500);
            panel.addNotify();
            try {
                layout(panel);
                panel.showPages(pages("book"));
                layout(panel);
                JList<?> list = (JList<?>) ((JScrollPane) panel.getComponent(1)).getViewport().getView();
                drain(replies);
                int initialEnd = list.getLastVisibleIndex() + 10;
                assertEquals(IntStream.rangeClosed(0, initialEnd).boxed().toList(), requested);
                requested.clear();
                panel.showSpread(101, 102);
                int first = list.getFirstVisibleIndex(), last = list.getLastVisibleIndex();
                drain(replies);
                assertEquals(IntStream.rangeClosed(first, last).boxed().toList(), requested.subList(0, last - first + 1));
                assertEquals(IntStream.rangeClosed(first - 10, last + 10).boxed().toList(), requested.stream().sorted().toList());
                requested.clear();
                panel.showSpread(1, 1);
                drain(replies);
                assertEquals(IntStream.rangeClosed(0, initialEnd).boxed().toList(), requested);
            } finally {
                panel.removeNotify();
            }
        });
    }
}
