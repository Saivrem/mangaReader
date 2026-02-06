package org.dustyroom.ui;

import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.FileUtils.isZipFile;
import static org.dustyroom.ui.LookSettings.SYSTEM;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;
import static org.dustyroom.ui.utils.UiUtils.redrawComponent;

public class ViewerController {
    private final JFrame frame;
    private final GraphicsDevice graphicsDevice;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;

    private ImageIterator imageIterator;

    private JComponent menuBar;
    private JComponent navigationPanel;
    private File currentDir;
    private boolean fullscreen;

    private boolean twoPageMode;
    private ReadingMode readingMode = ReadingMode.MANGA;

    private final List<Picture> pageHistory = new ArrayList<>();
    private int pageIndex = -1;

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane) {
        this.frame = frame;
        this.graphicsDevice = graphicsDevice;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
    }

    public void attachPanels(JComponent menuBar, JComponent navigationPanel) {
        this.menuBar = menuBar;
        this.navigationPanel = navigationPanel;
    }

    public void setImageIterator(ImageIterator imageIterator) {
        this.imageIterator = imageIterator;
        resetNavigationState();
    }

    public void chooseFile() {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(null);
        }

        String root = currentDir == null ? System.getProperty("user.home") : currentDir.toString();
        JFileChooser fileChooser = new JFileChooser(root);
        fileChooser.setPreferredSize(new Dimension(800, 600));

        if (UiUtils.getCurrent() != SYSTEM) {
            Action details = fileChooser.getActionMap().get("viewTypeDetails");
            if (details != null) {
                details.actionPerformed(null);
            }
        }

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Files", SUPPORTED_FORMATS);
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(FILES_ONLY);

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                setImageIterator(new DirImageIterator());
            } else if (isZipFile.test(selectedFile)) {
                setImageIterator(new ZipIterator(selectedFile));
            } else {
                setImageIterator(new FileImageIterator(selectedFile));
            }
            showNextImage();
        }

        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        }
        frame.requestFocus();
    }

    public void showNextImage() {
        if (imageIterator == null) return;

        if (pageHistory.isEmpty()) {
            Picture next = imageIterator.next();
            if (next == null) return;
            resetToSinglePage(next);
            renderCurrentPage();
            return;
        }

        int step = getSpreadStepForIndex(pageIndex, true);
        int targetIndex = pageIndex + step;

        if (!ensurePageLoaded(targetIndex)) return;
        pageIndex = targetIndex;
        renderCurrentPage();
    }

    public void showPreviousImage() {
        if (imageIterator == null) return;

        if (pageHistory.isEmpty()) {
            Picture prev = imageIterator.prev();
            if (prev == null) return;
            resetToSinglePage(prev);
            renderCurrentPage();
            return;
        }

        if (pageIndex > 0) {
            int targetIndex = findPreviousAnchorIndex(pageIndex);
            pageIndex = Math.max(0, targetIndex);
            renderCurrentPage();
            return;
        }

        Picture current = pageHistory.get(pageIndex);
        Picture prev = imageIterator.prev();
        if (prev == null) return;

        if (isSamePage(prev, current)) {
            prev = imageIterator.prev();
            if (prev == null || isSamePage(prev, current)) {
                return;
            }
        }

        // Crossing the left boundary resets navigation history to a clean baseline.
        resetToSinglePage(prev);
        renderCurrentPage();
    }

    public void showFirstImage() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture first = imageIterator.first();
        if (first == null) return;
        pageHistory.add(first);
        pageIndex = 0;
        renderCurrentPage();
    }

    public void showLastImage() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture last = imageIterator.last();
        if (last == null) return;
        pageHistory.add(last);
        pageIndex = 0;
        renderCurrentPage();
    }

    public void showNextVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture nextVol = imageIterator.nextVol();
        if (nextVol == null) return;
        pageHistory.add(nextVol);
        pageIndex = 0;
        renderCurrentPage();
    }

    public void showPrevVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture prevVol = imageIterator.prevVol();
        if (prevVol == null) return;
        pageHistory.add(prevVol);
        pageIndex = 0;
        renderCurrentPage();
    }

    public void processPicture(Picture picture) {
        resetNavigationState();
        if (picture == null) return;
        pageHistory.add(picture);
        pageIndex = 0;
        renderCurrentPage();
    }

    public void fitHeight() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_HEIGHT);
    }

    public void fitWidth() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_WIDTH);
    }

    public void fitScreen() {
        imageComponent.setFitMode(ImageComponent.FitMode.FIT_SCREEN);
    }

    public void zoomIn() {
        imageComponent.zoomIn();
    }

    public void zoomOut() {
        imageComponent.zoomOut();
    }

    public void toggleTwoPageMode() {
        twoPageMode = !twoPageMode;
        renderCurrentPage();
    }

    public void setComicsReadingMode() {
        readingMode = ReadingMode.COMICS;
        renderCurrentPage();
    }

    public void setMangaReadingMode() {
        readingMode = ReadingMode.MANGA;
        renderCurrentPage();
    }

    public void setNimbusTheme() {
        UiUtils.setDarkTheme();
        redrawComponent(frame);
    }

    public void setMetalTheme() {
        UiUtils.setMetalTheme();
        redrawComponent(frame);
    }

    public void setSystemTheme() {
        UiUtils.setSystemTheme();
        redrawComponent(frame);
    }

    public void showAboutDialog() {
        showAbout(frame);
    }

    public void exit() {
        System.exit(0);
    }

    public void toggleFullscreen() {
        if (fullscreen) {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(false);
            graphicsDevice.setFullScreenWindow(null);
            if (navigationPanel != null) navigationPanel.setVisible(true);
            if (menuBar != null) menuBar.setVisible(true);
            frame.setVisible(true);
        } else {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(true);
            graphicsDevice.setFullScreenWindow(frame);
            if (navigationPanel != null) navigationPanel.setVisible(false);
            if (menuBar != null) menuBar.setVisible(false);
            frame.setVisible(true);
        }

        fullscreen = !fullscreen;
        frame.requestFocusInWindow();
    }

    private void renderCurrentPage() {
        if (pageIndex < 0 || pageIndex >= pageHistory.size()) return;

        Picture current = pageHistory.get(pageIndex);
        Picture nextForSpread = resolveNextForSpread(pageIndex, true);

        PictureMetadata metadata = current.metadata();
        currentDir = metadata.dir();
        frame.setTitle(buildTitle(metadata, nextForSpread));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        BufferedImage imageToRender = composeDisplayImage(
                current.image(),
                nextForSpread == null ? null : nextForSpread.image()
        );
        imageComponent.setImageAndCenter(imageToRender, scrollPane);
    }

    private Picture resolveNextForSpread(int anchorIndex, boolean allowPrefetch) {
        if (!twoPageMode || anchorIndex < 0 || anchorIndex >= pageHistory.size()) {
            return null;
        }

        Picture current = pageHistory.get(anchorIndex);
        if (!isPortrait(current.image())) {
            return null;
        }

        int nextIndex = anchorIndex + 1;
        if (!ensurePageLoaded(nextIndex, allowPrefetch)) {
            return null;
        }

        Picture next = pageHistory.get(nextIndex);
        if (!isPortrait(next.image())) {
            return null;
        }

        return next;
    }

    private int getSpreadStepForIndex(int anchorIndex, boolean allowPrefetch) {
        return resolveNextForSpread(anchorIndex, allowPrefetch) == null ? 1 : 2;
    }

    private int findPreviousAnchorIndex(int currentAnchorIndex) {
        for (int candidate = currentAnchorIndex - 1; candidate >= 0; candidate--) {
            int candidateStep = getSpreadStepForIndex(candidate, false);
            if (candidate + candidateStep == currentAnchorIndex) {
                return candidate;
            }
        }
        return currentAnchorIndex - 1;
    }

    private boolean ensurePageLoaded(int pageIndexToLoad) {
        return ensurePageLoaded(pageIndexToLoad, true);
    }

    private boolean ensurePageLoaded(int pageIndexToLoad, boolean allowPrefetch) {
        if (pageIndexToLoad < pageHistory.size()) {
            return true;
        }
        if (!allowPrefetch || imageIterator == null) {
            return false;
        }

        while (pageHistory.size() <= pageIndexToLoad) {
            Picture lastLoaded = pageHistory.isEmpty() ? null : pageHistory.get(pageHistory.size() - 1);
            Picture next = imageIterator.next();
            if (next == null) {
                return false;
            }

            // Guard against iterator no-op on boundaries.
            if (lastLoaded != null && isSamePage(next, lastLoaded)) {
                next = imageIterator.next();
                if (next == null || isSamePage(next, lastLoaded)) {
                    return false;
                }
            }
            pageHistory.add(next);
        }
        return true;
    }

    private void resetToSinglePage(Picture picture) {
        if (picture == null) return;
        pageHistory.clear();
        pageHistory.add(picture);
        pageIndex = 0;
    }

    private boolean isSamePage(Picture first, Picture second) {
        if (first == null || second == null) return false;
        PictureMetadata firstMeta = first.metadata();
        PictureMetadata secondMeta = second.metadata();
        if (firstMeta == null || secondMeta == null) return false;

        File firstDir = firstMeta.dir();
        File secondDir = secondMeta.dir();
        String firstDirPath = firstDir == null ? "" : firstDir.getAbsolutePath();
        String secondDirPath = secondDir == null ? "" : secondDir.getAbsolutePath();

        return firstMeta.fileName().equals(secondMeta.fileName())
                && firstMeta.name().equals(secondMeta.name())
                && firstDirPath.equals(secondDirPath);
    }

    private boolean isPortrait(BufferedImage image) {
        return image != null && image.getHeight() >= image.getWidth();
    }

    private String buildTitle(PictureMetadata metadata, Picture second) {
        if (second == null) {
            return String.format("%s - %s", metadata.fileName(), metadata.name());
        }
        return String.format("%s - %s + %s", metadata.fileName(), metadata.name(), second.metadata().name());
    }

    private BufferedImage composeDisplayImage(BufferedImage firstImage, BufferedImage secondImage) {
        if (secondImage == null) return firstImage;

        BufferedImage leftImage = firstImage;
        BufferedImage rightImage = secondImage;
        if (readingMode == ReadingMode.MANGA) {
            leftImage = secondImage;
            rightImage = firstImage;
        }

        int width = leftImage.getWidth() + rightImage.getWidth();
        int height = Math.max(leftImage.getHeight(), rightImage.getHeight());

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);

        int leftY = (height - leftImage.getHeight()) / 2;
        int rightY = (height - rightImage.getHeight()) / 2;
        graphics.drawImage(leftImage, 0, leftY, null);
        graphics.drawImage(rightImage, leftImage.getWidth(), rightY, null);
        graphics.dispose();

        return canvas;
    }

    private void resetNavigationState() {
        pageHistory.clear();
        pageIndex = -1;
    }

    private enum ReadingMode {
        COMICS,
        MANGA
    }
}
