package org.dustyroom.ui.components;

import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.book.PictureMetadata;
import org.dustyroom.ui.listeners.MouseClickListener;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ThumbnailPanelTest {
    private static List<Integer> indices(int first, int last) {
        return IntStream
                .rangeClosed(first, last)
                .boxed()
                .toList();
    }

    private static void drain(List<Consumer<BufferedImage>> replies) {
        int limit = 100;
        while (!replies.isEmpty()) {
            assertTrue(limit-- > 0, "Loading must stop at the cache window boundary");
            replies.removeFirst().accept(new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB));
        }
    }

    private static List<PageRef> pages(String source) {
        return IntStream
                .range(0, 1000)
                .mapToObj(index -> {
                    String name = "page" + index;
                    return new PageRef(new PageKey(source, name), new PictureMetadata(name, source, new File(source)));
                })
                .toList();
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) {
                layout(container);
            }
        }
        parent.doLayout();
    }

    @Test
    void loadsVisiblePagesFirstOneAtATimeAndStopsWhenHidden() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> requested = new ArrayList<>();
            List<Consumer<BufferedImage>> replies = new ArrayList<>();
            ThumbnailPanel panel = new ThumbnailPanel((index, _, result) -> {
                requested.add(index);
                replies.add(result);
            }, _ -> {
            }, () -> {
            });
            panel.setSize(164, 500);
            panel.addNotify();
            try {
                layout(panel);
                panel.showPages(pages("book"));
                panel.showSpread(1, 2);
                assertEquals(List.of(0), requested);
                replies.getFirst().accept(new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB));
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
            ThumbnailPanel panel = new ThumbnailPanel((index, _, result) -> {
                requested.add(index);
                replies.add(result);
            }, _ -> {
            }, () -> {
            });
            panel.setSize(164, 500);
            panel.addNotify();
            try {
                layout(panel);
                panel.showPages(pages("book"));
                layout(panel);
                JList<?> list = (JList<?>) ((JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
                drain(replies);
                int initialEnd = list.getLastVisibleIndex() + 10;
                assertEquals(indices(0, initialEnd), requested);
                requested.clear();
                panel.showSpread(101, 102);
                int first = list.getFirstVisibleIndex(), last = list.getLastVisibleIndex();
                drain(replies);
                assertEquals(indices(first, last), requested.subList(0, last - first + 1));
                assertEquals(indices(first - 10, last + 10), requested
                        .stream()
                        .sorted()
                        .toList());
                requested.clear();
                panel.showSpread(1, 1);
                drain(replies);
                assertEquals(indices(0, initialEnd), requested);
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void gridWrapsAndEnterReturnsToSidebarBeforeNavigating() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger opened = new AtomicInteger();
            AtomicBoolean returned = new AtomicBoolean();
            ThumbnailPanel panel = new ThumbnailPanel((_, _, _) -> {
            }, page -> {
                assertTrue(returned.get(), "Restore the reader before opening the selected page");
                opened.set(page);
            }, () -> {
            });
            JPanel reader = new JPanel();
            JPanel navigation = new JPanel();
            ReaderWorkspace workspace = new ReaderWorkspace(panel, reader, navigation, grid -> returned.set(!grid));
            workspace.setSize(1000, 700);
            panel.showPages(pages("book"));
            panel.showSpread(1, 1);
            layout(workspace);
            panel.setGridMode(true);
            layout(workspace);
            JList<?> list = (JList<?>) ((JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
            assertTrue(panel.isGridMode());
            assertFalse(reader.isVisible());
            assertSame(workspace, reader.getParent(), "Hidden reader still receives theme updates");
            assertFalse(navigation.isVisible());
            assertEquals(list.getCellBounds(0, 0).y, list.getCellBounds(1, 1).y, "list=" + list.getSize() + " panel=" + panel.getSize() + " mode=" + list.getLayoutOrientation());
            assertTrue(list.getCellBounds(1, 1).x > list.getCellBounds(0, 0).x);
            list.setSelectedIndex(450);
            list.getActionMap().get("openPage").actionPerformed(null);
            layout(workspace);
            assertEquals(451, opened.get());
            assertFalse(panel.isGridMode());
            assertEquals(JList.VERTICAL, list.getLayoutOrientation());
            assertTrue(reader.isVisible());
            assertTrue(navigation.isVisible());
            assertSame(panel, ((BorderLayout) workspace.getLayout()).getLayoutComponent(BorderLayout.WEST));
        });
    }

    @Test
    void escapeReturnsWithoutOpeningPageAndPreservesHiddenFullscreenControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger opened = new AtomicInteger();
            ThumbnailPanel panel = new ThumbnailPanel((_, _, _) -> {
            }, opened::set, () -> {
            });
            JPanel navigation = new JPanel();
            navigation.setVisible(false);
            ReaderWorkspace workspace = new ReaderWorkspace(panel, new JPanel(), navigation, _ -> {
            });
            workspace.setSize(1000, 700);
            panel.setGridMode(true);
            assertFalse(panel.isGridMode(), "Empty books cannot enter the grid");
            panel.showPages(pages("book"));
            panel.showSpread(20, 21);
            panel.setGridMode(true);
            layout(workspace);
            assertEquals("closeGrid", panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
            panel.getActionMap().get("closeGrid").actionPerformed(null);
            assertFalse(panel.isGridMode());
            assertFalse(navigation.isVisible());
            assertEquals(0, opened.get());
            assertNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
        });
    }

    @Test
    void gridLoadsOnlyViewportAndMarginAndStopsAfterCollapse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Integer> requested = new ArrayList<>();
            List<Consumer<BufferedImage>> replies = new ArrayList<>();
            ThumbnailPanel panel = new ThumbnailPanel((index, _, result) -> {
                requested.add(index);
                replies.add(result);
            }, _ -> {
            }, () -> {
            });
            ReaderWorkspace workspace = new ReaderWorkspace(panel, new JPanel(), new JPanel(), _ -> {
            });
            workspace.setSize(1000, 700);
            workspace.addNotify();
            try {
                layout(workspace);
                panel.showPages(pages("book"));
                panel.showSpread(1, 1);
                panel.setGridMode(true);
                layout(workspace);
                JList<?> list = (JList<?>) ((JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
                drain(replies);
                int visibleEnd = list.getLastVisibleIndex();
                assertTrue(visibleEnd > 2);
                assertEquals(indices(0, visibleEnd + 10), requested);
                panel.setExpanded(false);
                assertFalse(panel.isGridMode());
                int loaded = requested.size();
                drain(replies);
                assertEquals(loaded, requested.size());
            } finally {
                workspace.removeNotify();
            }
        });
    }

    @Test
    void mouseSelectionReturnsAndGridAdaptsToWindowWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger opened = new AtomicInteger();
            ThumbnailPanel panel = new ThumbnailPanel((_, _, _) -> {
            }, opened::set, () -> {
            });
            ReaderWorkspace workspace = new ReaderWorkspace(panel, new JPanel(), new JPanel(), _ -> {
            });
            workspace.setSize(1000, 700);
            panel.showPages(pages("book"));
            panel.setGridMode(true);
            layout(workspace);
            JList<?> list = (JList<?>) ((JScrollPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
            assertEquals(list.getCellBounds(0, 0).y, list.getCellBounds(4, 4).y);
            workspace.setSize(500, 700);
            layout(workspace);
            assertTrue(list.getCellBounds(4, 4).y > list.getCellBounds(0, 0).y);
            Rectangle cell = list.getCellBounds(4, 4);
            MouseEvent click = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0,
                    cell.x + 8, cell.y + 8, 0, 0, 1, false, MouseEvent.BUTTON1);
            for (MouseListener listener : list.getMouseListeners()) {
                if (listener instanceof MouseClickListener) {
                    listener.mouseClicked(click);
                }
            }
            assertEquals(5, opened.get());
            assertFalse(panel.isGridMode());
            panel.setGridMode(true);
            panel.showPages(pages("another"));
            assertFalse(panel.isGridMode(), "Changing books must restore the reading workspace");
        });
    }
}
