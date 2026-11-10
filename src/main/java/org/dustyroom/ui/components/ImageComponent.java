package org.dustyroom.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Timer;
import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.animation.AnimationFrame;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.ui.loading.BufferedAnimation;

public class ImageComponent extends JComponent {
    private final Timer animationTimer = new Timer(10, event -> animate());
    private BufferedImage image;
    private List<AnimatedPage> animations = List.of();
    private List<BufferedAnimation> players = List.of();
    private boolean animationPaused;
    private double scale = 1.0;
    private FitMode fitMode = FitMode.FIT_SCREEN;

    public ImageComponent() {
        addHierarchyListener(event -> {
            if (isShowing()) {
                startAnimations();
            } else {
                stopAnimations();
            }
        });
    }

    private void animate() {
        long now = System.nanoTime();
        boolean changed = false;
        for (BufferedAnimation player : players) {
            AnimationFrame previous = player.currentFrame();
            if (player.advance(now) != previous) {
                changed = true;
            }
        }
        if (players.stream().allMatch(player -> player.finished(now))) {
            animationTimer.stop();
        }
        if (changed) {
            repaint();
        }
    }

    private void startAnimations() {
        if (animationPaused || !players.isEmpty() || animations.isEmpty()) {
            return;
        }
        players = animations.stream().map(page -> new BufferedAnimation(page.getClip(), error ->
                Logger.getLogger(ImageComponent.class.getName())
                        .log(Level.WARNING, "Cannot play GIF", error))).toList();
        animationTimer.start();
    }

    private void stopAnimations() {
        animationTimer.stop();
        players.forEach(BufferedAnimation::close);
        players = List.of();
    }

    public void setAnimationPaused(boolean paused) {
        animationPaused = paused;
        if (paused) {
            stopAnimations();
        } else if (isShowing()) {
            startAnimations();
        }
    }

    public void setAnimations(List<AnimatedPage> pages) {
        stopAnimations();
        animations = List.copyOf(pages);
        if (isShowing()) {
            startAnimations();
        }
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
        animationPaused = false;
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
        if (image == null) {
            return;
        }

        Dimension parentSize = getParent() != null
                ? getParent().getSize()
                : Toolkit.getDefaultToolkit().getScreenSize();

        if (parentSize.width <= 0 || parentSize.height <= 0) {
            return;
        }
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
        if (image == null || !Double.isFinite(factor) || factor <= 0) {
            return;
        }
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
        if (image == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int imgW = (int) (image.getWidth() * scale);
        int imgH = (int) (image.getHeight() * scale);

        int x = (getWidth() - imgW) / 2;
        int y = (getHeight() - imgH) / 2;

        // Do not rescale a full static image only to cover it with the animated frame.
        // In spreads retain the still page and the black margins around each page.
        Area stillArea = new Area(new Rectangle(x, y, imgW, imgH));
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).currentFrame() != null) {
                AnimatedPage page = animations.get(i);
                stillArea.subtract(new Area(animationBounds(page, x, y)));
            }
        }
        if (!stillArea.isEmpty()) {
            Graphics2D stillGraphics = (Graphics2D) g2d.create();
            try {
                stillGraphics.clip(stillArea);
                stillGraphics.drawImage(image, x, y, imgW, imgH, this);
            } finally {
                stillGraphics.dispose();
            }
        }
        // Bicubic resampling of every moving frame monopolizes the EDT on large GIFs.
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int i = 0; i < players.size(); i++) {
            AnimationFrame frame = players.get(i).currentFrame();
            if (frame == null) {
                continue;
            }
            AnimatedPage page = animations.get(i);
            Rectangle bounds = animationBounds(page, x, y);
            int px = bounds.x, py = bounds.y, width = bounds.width, height = bounds.height;
            g2d.setColor(page.isSpread() ? Color.BLACK : getBackground());
            g2d.fillRect(px, py, width, height);
            g2d.drawImage(frame.getImage(), px, py, width, height, this);
        }
    }

    private Rectangle animationBounds(AnimatedPage page, int x, int y) {
        return new Rectangle(x + (int) Math.round(page.getX() * scale),
                y + (int) Math.round(page.getY() * scale),
                (int) Math.round(page.getWidth() * scale), (int) Math.round(page.getHeight() * scale));
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(0, 0);
        }
        return new Dimension(
                (int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale)
        );
    }

}
