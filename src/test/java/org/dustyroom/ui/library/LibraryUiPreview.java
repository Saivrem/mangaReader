package org.dustyroom.ui.library;

import org.dustyroom.be.models.library.LibraryChapter;
import org.dustyroom.be.models.library.LibrarySnapshot;
import org.dustyroom.be.models.library.LibrarySource;
import org.dustyroom.be.models.library.LibraryWork;
import org.dustyroom.be.models.state.ViewerTheme;
import org.dustyroom.be.models.view.ViewerFrame;
import org.dustyroom.ui.loading.AsyncViewerSession;
import org.dustyroom.ui.loading.ViewerView;
import org.dustyroom.ui.utils.UiUtils;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Explicit headless preview; does not open or modify the real reader database. */
public final class LibraryUiPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of("build/ui-preview");
        Files.createDirectories(output);
        AsyncViewerSession session = new AsyncViewerSession(new ViewerView() {
            public void showLoading() { }
            public void showFrame(ViewerFrame frame) { }
            public void showError(String message) { }
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (ViewerTheme theme : ViewerTheme.values()) {
                    UiUtils.installTheme(theme);
                    LibraryController controller = new LibraryController(session, ignored -> { });
                    LibraryPanel panel = controller.panel();
                    List<LibraryWork> works = new ArrayList<>();
                    for (int index = 0; index < 3; index++) {
                        LibrarySource source = new LibrarySource(output.resolve("demo-" + index + ".zip").toAbsolutePath().toString(), "Chapter 01.zip", false);
                        LibraryChapter chapter = new LibraryChapter("chapter" + index, source, index == 1, index == 0 ? 8 : 0, index == 0 ? 24 : 0, index == 0 ? 1 : 0);
                        works.add(new LibraryWork("work" + index, List.of("Night train · 夜行", "The quiet city", "A distant shore").get(index),
                                output.toAbsolutePath().toString(), index, List.of(chapter)));
                    }
                    panel.showSnapshot(new LibrarySnapshot(works, Set.of("chapter0", "chapter1")));
                    panel.setSize(940, 560);
                    layout(panel);
                    write(panel, output.resolve("library-" + theme.name().toLowerCase() + ".png"));
                    findButton(panel, "Night train · 夜行").doClick();
                    layout(panel);
                    write(panel, output.resolve("chapters-" + theme.name().toLowerCase() + ".png"));
                }
            });
        } finally { session.close(); }
    }

    private static JButton findButton(Container parent, String title) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && title.equals(button.getText())) return button;
            if (component instanceof Container container) {
                JButton found = findButton(container, title);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static void write(JComponent component, Path file) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.printAll(graphics); } finally { graphics.dispose(); }
        try { ImageIO.write(image, "png", file.toFile()); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
