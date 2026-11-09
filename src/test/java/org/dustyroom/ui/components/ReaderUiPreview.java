package org.dustyroom.ui.components;

import org.dustyroom.ui.actions.ViewerActions;
import org.dustyroom.ui.actions.ViewerActionState;
import org.dustyroom.ui.loading.ViewerFrame;
import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.ui.theme.ViewerTheme;
import org.dustyroom.ui.utils.UiUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Headless visual fixture; writes previews only when explicitly run. */
public final class ReaderUiPreview {
    public static void main(String[] args) throws Exception {
        File output = new File("build/ui-preview");
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        org.dustyroom.ui.BrandAssets.load();
        SwingUtilities.invokeAndWait(() -> {
            for (ViewerTheme theme : ViewerTheme.values()) {
                if (!UiUtils.installTheme(theme)) throw new IllegalStateException("Cannot install " + theme);
                renderAbout(output, theme);
                for (boolean loaded : new boolean[]{false, true}) {
                    render(output, theme, loaded, 1100);
                    render(output, theme, loaded, 940);
                }
            }
        });
    }

    private static void renderAbout(File output, ViewerTheme theme) {
        JOptionPane pane = new JOptionPane(org.dustyroom.be.utils.Constants.ABOUT.formatted("0.9.0"),
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                org.dustyroom.ui.BrandAssets.aboutIcon());
        pane.setSize(520, 240);
        layout(pane);
        BufferedImage screenshot = new BufferedImage(1040, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = screenshot.createGraphics();
        try { graphics.scale(2, 2); pane.printAll(graphics); } finally { graphics.dispose(); }
        try { ImageIO.write(screenshot, "png", new File(output, theme.name().toLowerCase() + "-about.png")); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }

    private static void render(File output, ViewerTheme theme, boolean loaded, int width) {
        ViewerActions actions = actions();
        ViewerFrame frame = loaded ? new ViewerFrame(samplePage(), "Sample book", null,
                24, 25, 180, true, ReadingMode.MANGA, null) : null;
        ViewerActionState.apply(actions, frame, ImageComponent.FitMode.FIT_SCREEN, false);
        ImageComponent image = new ImageComponent();
        image.setOpaque(true);
        image.setBackground(UIManager.getColor("Viewer.background"));
        JScrollPane scroll = new JScrollPane(image);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UIManager.getColor("Viewer.background"));
        ReaderSurface surface = new ReaderSurface(scroll, actions.openFile());
        if (!loaded) surface.showRecentBooks(java.util.stream.IntStream.range(0, 6).mapToObj(i ->
                new org.dustyroom.ui.loading.RecentBook(new org.dustyroom.persistence.BookState(
                        "/books/" + (i == 0 ? "日本語 — The long way home.zip" : "Volume " + (i + 1) + ".zip"),
                        "page-24", 24 + i, 180, true, ReadingMode.MANGA), i != 2,
                        i == 2 ? null : org.dustyroom.ui.rendering.ThumbnailRenderer.render(samplePage()))).toList());
        NavigationPanel navigation = new NavigationPanel(actions);
        navigation.updateState(frame, ImageComponent.FitMode.FIT_SCREEN);
        JPanel content = new JPanel(new BorderLayout());
        ThumbnailPanel thumbnails = new ThumbnailPanel((index, page, result) ->
                result.accept(org.dustyroom.ui.rendering.ThumbnailRenderer.render(samplePage())), page -> {}, () -> {});
        if (loaded) content.add(thumbnails, BorderLayout.WEST);
        content.add(surface, BorderLayout.CENTER);
        content.add(navigation, BorderLayout.SOUTH);
        JRootPane root = new JRootPane();
        root.setJMenuBar(new MenuBar(actions, UiUtils::installTheme));
        root.setContentPane(content);
        JPanel host = new JPanel(new BorderLayout());
        host.add(root);
        host.setSize(width, 720);
        root.setSize(width, 720);
        if (loaded) surface.showReading();
        if (loaded) {
            host.addNotify();
            thumbnails.showPages(java.util.stream.IntStream.range(0, 180).mapToObj(i ->
                    new org.dustyroom.be.models.PageRef(new org.dustyroom.be.models.PageKey("preview", "page" + i),
                            new org.dustyroom.be.models.PictureMetadata("page" + i, "preview", new File(".")))).toList());
        }
        layout(root);
        if (loaded) thumbnails.showSpread(24, 25);
        if (loaded) { image.setImageAndCenter(frame.image(), scroll); layout(root); }
        BufferedImage screenshot = new BufferedImage(width * 2, 1440, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = screenshot.createGraphics();
        try { graphics.scale(2, 2); root.printAll(graphics); } finally { graphics.dispose(); }
        try { ImageIO.write(screenshot, "png", new File(output,
                theme.name().toLowerCase() + (loaded ? "-reading-" : "-welcome-") + width + ".png")); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
        if (loaded) host.removeNotify();
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static BufferedImage samplePage() {
        BufferedImage page = new BufferedImage(850, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = page.createGraphics();
        try {
            g.setColor(new Color(0xF3F1EC)); g.fillRect(0, 0, 850, 600);
            g.setColor(new Color(0x272D35));
            g.setStroke(new BasicStroke(3));
            g.drawRect(28, 28, 380, 540); g.drawRect(442, 28, 380, 540);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.drawString("CHAPTER 03", 65, 90);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            g.drawString("The long way home", 65, 123);
            for (int x : new int[]{55, 470}) {
                g.drawRect(x, 160, 325, 180);
                g.drawLine(x, 310, x + 110, 210); g.drawLine(x + 110, 210, x + 240, 310);
                g.drawLine(x + 175, 260, x + 245, 195); g.drawLine(x + 245, 195, x + 325, 280);
                g.drawOval(x + 250, 180, 30, 30);
                g.drawRect(x, 360, 150, 165); g.drawRect(x + 175, 360, 150, 165);
            }
            g.drawString("24", 207, 588); g.drawString("25", 622, 588);
        } finally { g.dispose(); }
        return page;
    }

    private static ViewerActions actions() {
        Map<String, Action> values = new HashMap<>();
        return (ViewerActions) Proxy.newProxyInstance(ViewerActions.class.getClassLoader(),
                new Class<?>[]{ViewerActions.class}, (proxy, method, arguments) ->
                        values.computeIfAbsent(method.getName(), name -> new AbstractAction(name) {
                            @Override public void actionPerformed(ActionEvent event) {}
                        }));
    }
}
