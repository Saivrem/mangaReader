package org.dustyroom.ui.components;

import org.dustyroom.be.animation.AnimationFrame;
import org.dustyroom.ui.loading.AnimatedPage;
import org.dustyroom.ui.loading.BufferedAnimation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageComponent extends JComponent {
    private final Timer animationTimer = new Timer(20, event -> animate());
    private BufferedImage image;
    private List<AnimatedPage> animations = List.of();
    private List<BufferedAnimation> players = List.of();
    private double scale = 1.0;
    private FitMode fitMode = FitMode.FIT_SCREEN;

    public ImageComponent() {
        addHierarchyListener(event -> {
            if (isShowing()) startAnimations();
            else stopAnimations();
        });
    }

    private void animate() {
        long now = System.nanoTime();
        players.forEach(player -> player.advance(now));
        if (players.stream().allMatch(player -> player.finished(now))) animationTimer.stop();
        repaint();
    }

    private void startAnimations() {
        if (!players.isEmpty() || animations.isEmpty()) return;
        players = animations.stream().map(page -> new BufferedAnimation(page.clip(), error ->
                Logger.getLogger(ImageComponent.class.getName())
                        .log(Level.WARNING, "Cannot play GIF", error))).toList();
        animationTimer.start();
    }

    private void stopAnimations() {
        animationTimer.stop();
        players.forEach(BufferedAnimation::close);
        players = List.of();
    }

    public void setAnimations(List<AnimatedPage> pages) {
        stopAnimations();
        animations = List.copyOf(pages);
        if (isShowing()) startAnimations();
        repaint();
    }

    @Override
    public void removeNotify() {
        stopAnimations();
        super.removeNotify();
    }

    public void setImageAndCenter(BufferedImage img, JScrollPane scrollPane) {
        setImage(img);

        // Finish layout before painting: no intermediate frame at x=0 or stale sidebar width.
        for (int pass = 0; pass < 3; pass++) {
            scrollPane.doLayout();
            scrollPane.getViewport().doLayout();
            updateScale();
            setSize(getPreferredSize().width < scrollPane.getViewport().getWidth()
                            ? scrollPane.getViewport().getWidth() : getPreferredSize().width,
                    Math.max(scrollPane.getViewport().getHeight(), getPreferredSize().height));
        }
        Dimension viewSize = scrollPane.getViewport().getExtentSize();
        scrollPane.getViewport().setViewPosition(new Point(Math.max(0, (getPreferredSize().width - viewSize.width) / 2), 0));
    }

    private void setImage(BufferedImage image) {
        stopAnimations();
        animations = List.of();
        this.image = image;
        updateScale();
        revalidate();
        repaint();
    }

    public FitMode getFitMode() {
        return fitMode;
    }

    public void setFitMode(FitMode mode) {
        this.fitMode = mode;
        updateScale();
        revalidate();
        repaint();
    }

    public double getScale() {
        return scale;
    }

    public void restoreView(FitMode mode, double zoom) {
        fitMode = mode;
        scale = zoom;
        updateScale();
        revalidate();
        repaint();
    }

    public void updateScale() {
        if (image == null) return;

        Dimension parentSize = getParent() != null
                ? getParent().getSize()
                : Toolkit.getDefaultToolkit().getScreenSize();

        if (parentSize.width <= 0 || parentSize.height <= 0) return;
        switch (fitMode) {
            case FIT_WIDTH -> scale = parentSize.getWidth() / image.getWidth();
            case FIT_HEIGHT -> scale = parentSize.getHeight() / image.getHeight();
            case FIT_SCREEN -> scale = Math.min(
                    parentSize.getWidth() / image.getWidth(),
                    parentSize.getHeight() / image.getHeight()
            );
            case ORIGINAL -> { /* Preserve the explicit zoom across page changes. */ }
        }
    }

    public void zoomAt(double factor, Point viewportAnchor, JScrollPane scroll) {
        if (image == null || !Double.isFinite(factor) || factor <= 0) return;
        JViewport viewport = scroll.getViewport();
        double oldScale = scale;
        Point oldPosition = viewport.getViewPosition();
        double imageX = (oldPosition.x + viewportAnchor.x - (getWidth() - image.getWidth() * oldScale) / 2) / oldScale;
        double imageY = (oldPosition.y + viewportAnchor.y - (getHeight() - image.getHeight() * oldScale) / 2) / oldScale;
        fitMode = FitMode.ORIGINAL;
        scale = Math.max(0.05, Math.min(20, scale * factor));
        revalidate();
        scroll.doLayout();
        viewport.doLayout();
        setSize(Math.max(viewport.getWidth(), getPreferredSize().width), Math.max(viewport.getHeight(), getPreferredSize().height));
        int x = (int) Math.round(imageX * scale + (getWidth() - image.getWidth() * scale) / 2 - viewportAnchor.x);
        int y = (int) Math.round(imageY * scale + (getHeight() - image.getHeight() * scale) / 2 - viewportAnchor.y);
        viewport.setViewPosition(new Point(Math.max(0, Math.min(x, getWidth() - viewport.getWidth())),
                Math.max(0, Math.min(y, getHeight() - viewport.getHeight()))));
        repaint();
    }

    public void zoomIn() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.max(0.05, Math.min(20, scale * 1.1));
        revalidate();
        repaint();
    }

    public void zoomOut() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.max(0.05, Math.min(20, scale / 1.1));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // JComponent has no UI delegate to clear an opaque background for us.
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (image == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int imgW = (int) (image.getWidth() * scale);
        int imgH = (int) (image.getHeight() * scale);

        int x = (getWidth() - imgW) / 2;
        int y = (getHeight() - imgH) / 2;

        g2d.drawImage(image, x, y, imgW, imgH, this);
        for (int i = 0; i < players.size(); i++) {
            AnimationFrame frame = players.get(i).advance(System.nanoTime());
            if (frame == null) continue;
            AnimatedPage page = animations.get(i);
            int px = x + (int) Math.round(page.x() * scale), py = y + (int) Math.round(page.y() * scale);
            int width = (int) Math.round(page.width() * scale), height = (int) Math.round(page.height() * scale);
            g2d.setColor(page.spread() ? Color.BLACK : getBackground());
            g2d.fillRect(px, py, width, height);
            g2d.drawImage(frame.image(), px, py, width, height, this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) return new Dimension(0, 0);
        return new Dimension(
                (int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale)
        );
    }


}
