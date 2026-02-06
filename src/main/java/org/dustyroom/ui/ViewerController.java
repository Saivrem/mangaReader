package org.dustyroom.ui;

import org.dustyroom.be.iterators.DirImageIterator;
import org.dustyroom.be.iterators.FileImageIterator;
import org.dustyroom.be.iterators.ImageIterator;
import org.dustyroom.be.iterators.ZipIterator;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;
import org.dustyroom.ui.components.ImageComponent;
import org.dustyroom.ui.navigation.ReadingMode;
import org.dustyroom.ui.navigation.SpreadPlanner;
import org.dustyroom.ui.navigation.ViewerNavigationState;
import org.dustyroom.ui.rendering.SpreadRenderer;
import org.dustyroom.ui.utils.UiUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static javax.swing.JFileChooser.FILES_ONLY;
import static org.dustyroom.be.utils.Constants.SUPPORTED_FORMATS;
import static org.dustyroom.be.utils.FileUtils.isZipFile;
import static org.dustyroom.ui.LookSettings.NIMBUS;
import static org.dustyroom.ui.LookSettings.SYSTEM;
import static org.dustyroom.ui.navigation.ReadingMode.COMICS;
import static org.dustyroom.ui.navigation.ReadingMode.MANGA;
import static org.dustyroom.ui.utils.DialogUtils.showAbout;
import static org.dustyroom.ui.utils.UiUtils.redrawComponent;

public class ViewerController {
    private final JFrame frame;
    private final GraphicsDevice graphicsDevice;
    private final ImageComponent imageComponent;
    private final JScrollPane scrollPane;
    private final SpreadPlanner spreadPlanner;
    private final SpreadRenderer spreadRenderer;

    private ImageIterator imageIterator;

    private JComponent menuBar;
    private JComponent navigationPanel;
    private File currentDir;
    private boolean fullscreen;

    private boolean twoPageMode;
    private ReadingMode readingMode = MANGA;
    private ViewerNavigationState state = new ViewerNavigationState();

    public ViewerController(JFrame frame, GraphicsDevice graphicsDevice, ImageComponent imageComponent, JScrollPane scrollPane) {
        this.frame = frame;
        this.graphicsDevice = graphicsDevice;
        this.imageComponent = imageComponent;
        this.scrollPane = scrollPane;
        this.spreadPlanner = new SpreadPlanner();
        this.spreadRenderer = new SpreadRenderer();
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

        if (pageHistory().isEmpty()) {
            Picture next = imageIterator.next();
            if (next == null) return;
            resetToSinglePage(next);
            renderCurrentPage();
            return;
        }

        Picture current = pageHistory().get(pageIndex());
        Picture secondForSpread = resolveNextForSpread(pageIndex());
        int step = spreadPlanner.resolveStep(current, secondForSpread);
        int targetIndex = pageIndex() + step;

        if (pageNotLoaded(targetIndex)) return;
        state.setPageIndex(targetIndex);
        renderCurrentPage();
    }

    public void showPreviousImage() {
        if (imageIterator == null) return;

        if (pageHistory().isEmpty()) {
            Picture prev = imageIterator.prev();
            if (prev == null) return;
            resetToSinglePage(prev);
            renderCurrentPage();
            return;
        }

        if (pageIndex() > 0) {
            int targetIndex = spreadPlanner.findPreviousAnchorIndex(pageHistory(), pageIndex(), twoPageMode);
            state.setPageIndex(Math.max(0, targetIndex));
            renderCurrentPage();
            return;
        }

        Picture current = pageHistory().get(pageIndex());
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
        pageHistory().add(first);
        state.setPageIndex(0);
        renderCurrentPage();
    }

    public void showLastImage() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture last = imageIterator.last();
        if (last == null) return;
        pageHistory().add(last);
        state.setPageIndex(0);
        renderCurrentPage();
    }

    public void showNextVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture nextVol = imageIterator.nextVol();
        if (nextVol == null) return;
        pageHistory().add(nextVol);
        state.setPageIndex(0);
        renderCurrentPage();
    }

    public void showPrevVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        Picture prevVol = imageIterator.prevVol();
        if (prevVol == null) return;
        pageHistory().add(prevVol);
        state.setPageIndex(0);
        renderCurrentPage();
    }

    public void processPicture(Picture picture) {
        resetNavigationState();
        if (picture == null) return;
        pageHistory().add(picture);
        state.setPageIndex(0);
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
        readingMode = COMICS;
        renderCurrentPage();
    }

    public void setMangaReadingMode() {
        readingMode = MANGA;
        renderCurrentPage();
    }

    public void setNimbusTheme() {
        UiUtils.setDarkTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
    }

    public void setMetalTheme() {
        UiUtils.setMetalTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
    }

    public void setSystemTheme() {
        UiUtils.setSystemTheme();
        redrawComponent(frame);
        syncThemeSurfaces();
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

    public void syncThemeSurfaces() {
        Color background = resolveThemeBackground();

        Container contentPane = frame.getContentPane();
        contentPane.setBackground(background);

        Border empty = BorderFactory.createEmptyBorder();
        scrollPane.setBorder(empty);
        scrollPane.setViewportBorder(empty);
        scrollPane.setOpaque(true);
        if (background == null) {
            background = new Color(34, 40, 49);
        }

        imageComponent.setOpaque(true);
        imageComponent.setBackground(background);
        scrollPane.setBackground(background);

        JViewport viewport = scrollPane.getViewport();
        viewport.setOpaque(true);
        viewport.setBackground(background);
    }

    private Color resolveThemeBackground() {
        if (UiUtils.getCurrent() == NIMBUS) {
            Color nimbusBackground = UIManager.getColor("background");
            if (nimbusBackground != null) {
                return nimbusBackground;
            }
            return new Color(34, 40, 49);
        }

        Color panelBackground = UIManager.getColor("Panel.background");
        if (panelBackground != null) {
            return panelBackground;
        }

        return UIManager.getColor("control");
    }

    private void renderCurrentPage() {
        if (pageIndex() < 0 || pageIndex() >= pageHistory().size()) return;

        Picture current = pageHistory().get(pageIndex());
        Picture nextForSpread = resolveNextForSpread(pageIndex());

        PictureMetadata metadata = current.metadata();
        currentDir = metadata.dir();
        frame.setTitle(spreadRenderer.buildTitle(metadata, nextForSpread));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        BufferedImage imageToRender = spreadRenderer.compose(
                current.image(),
                nextForSpread == null ? null : nextForSpread.image(),
                readingMode == MANGA
        );
        imageComponent.setImageAndCenter(imageToRender, scrollPane);
    }

    private Picture resolveNextForSpread(int anchorIndex) {
        if (anchorIndex < 0 || anchorIndex >= pageHistory().size()) {
            return null;
        }

        Picture current = pageHistory().get(anchorIndex);
        int nextIndex = anchorIndex + 1;
        if (pageNotLoaded(nextIndex)) {
            return null;
        }

        Picture next = pageHistory().get(nextIndex);
        return spreadPlanner.resolveSecondPage(current, next, twoPageMode);
    }

    private boolean pageNotLoaded(int pageIndexToLoad) {
        if (pageIndexToLoad < pageHistory().size()) {
            return false;
        }
        if (imageIterator == null) {
            return true;
        }

        while (pageHistory().size() <= pageIndexToLoad) {
            Picture lastLoaded = pageHistory().isEmpty() ? null : pageHistory().get(pageHistory().size() - 1);
            Picture next = imageIterator.next();
            if (next == null) {
                return true;
            }

            // Guard against iterator no-op on boundaries.
            if (lastLoaded != null && isSamePage(next, lastLoaded)) {
                next = imageIterator.next();
                if (next == null || isSamePage(next, lastLoaded)) {
                    return true;
                }
            }
            pageHistory().add(next);
        }
        return false;
    }

    private void resetToSinglePage(Picture picture) {
        if (picture == null) return;
        state = new ViewerNavigationState();
        pageHistory().add(picture);
        state.setPageIndex(0);
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

    private void resetNavigationState() {
        state = new ViewerNavigationState();
    }

    private List<Picture> pageHistory() {
        return state.pageHistory();
    }

    private int pageIndex() {
        return state.pageIndex();
    }
}
