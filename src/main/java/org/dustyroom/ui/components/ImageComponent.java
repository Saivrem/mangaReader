package org.dustyroom.ui.components;

import lombok.Getter;

import org.dustyroom.be.models.animation.AnimatedPage;
import org.dustyroom.be.models.animation.AnimationFrame;
import org.dustyroom.be.models.state.FitMode;
import org.dustyroom.ui.loading.BufferedAnimation;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageComponent extends JComponent {
    private final Timer animationTimer = new Timer(10, _ -> animate());
    private BufferedImage image;
    private List<AnimatedPage> animations = List.of();
    private List<BufferedAnimation> players = List.of();
    private boolean animationPaused;
    @Getter
    private double scale = 1.0;
    @Getter
    private FitMode fitMode = FitMode.FIT_SCREEN;

    public ImageComponent() {
        addHierarchyListener(_ -> {
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
        boolean finished = true;
        for (BufferedAnimation player : players) {
            AnimationFrame previous = player.currentFrame();
            if (player.advance(now) != previous) {
                changed = true;
            }
            if (!player.finished(now)) {
                finished = false;
            }
        }

        if (finished) {
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
        Logger logger = Logger.getLogger(ImageComponent.class.getName());
        players = animations
                .stream()
                .map(page -> new BufferedAnimation(page.getClip(), error -> logger.log(Level.WARNING, "Cannot play GIF", error)))
                .toList();

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

        JViewport viewport = scrollPane.getViewport();
        // Finish layout before painting: no intermediate frame at x=0 or stale sidebar width.
        for (int pass = 0; pass < 3; pass++) {
            scrollPane.doLayout();
            viewport.doLayout();
            updateScale();
            sizeToViewport(viewport);
        }

        int centerX = Math.max(0, (getWidth() - viewport.getExtentSize().width) / 2);
        viewport.setViewPosition(new Point(centerX, 0));
    }

    private void sizeToViewport(JViewport viewport) {
        Dimension preferred = getPreferredSize();
        setSize(Math.max(preferred.width, viewport.getWidth()),
                Math.max(preferred.height, viewport.getHeight()));
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

    public void setFitMode(FitMode mode) {
        this.fitMode = mode;
        updateScale();
        revalidate();
        repaint();
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
        scale = Math.clamp(scale * factor, 0.05, 20);
        revalidate();
        scroll.doLayout();
        viewport.doLayout();
        sizeToViewport(viewport);

        int x = (int) Math.round(imageX * scale + (getWidth() - image.getWidth() * scale) / 2 - viewportAnchor.x);
        int y = (int) Math.round(imageY * scale + (getHeight() - image.getHeight() * scale) / 2 - viewportAnchor.y);
        setViewportPosition(viewport, x, y);
    }

    void setViewportPosition(JViewport viewport, int x, int y) {
        int maxX = Math.max(0, getWidth() - viewport.getWidth());
        int maxY = Math.max(0, getHeight() - viewport.getHeight());
        viewport.setViewPosition(new Point(Math.clamp(x, 0, maxX), Math.clamp(y, 0, maxY)));
        repaint();
    }

    public void zoomIn() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.clamp(scale * 1.1, 0.05, 20);
        revalidate();
        repaint();
    }

    public void zoomOut() {
        fitMode = FitMode.ORIGINAL;
        scale = Math.clamp(scale / 1.1, 0.05, 20);
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
