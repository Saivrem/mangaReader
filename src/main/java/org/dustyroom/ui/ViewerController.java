package org.dustyroom.ui;

import lombok.Setter;
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

    @Setter
    private ImageIterator imageIterator;

    private JComponent menuBar;
    private JComponent navigationPanel;
    private File currentDir;
    private boolean fullscreen;

    private boolean twoPageMode;
    private ReadingMode readingMode = ReadingMode.COMICS;

    private final List<Spread> history = new ArrayList<>();
    private int historyIndex = -1;
    private Picture bufferedNext;

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

        if (historyIndex + 1 < history.size()) {
            historyIndex++;
            renderSpread(history.get(historyIndex));
            return;
        }

        Spread spread = createNextSpread();
        if (spread == null) return;

        if (historyIndex + 1 < history.size()) {
            history.subList(historyIndex + 1, history.size()).clear();
        }
        history.add(spread);
        historyIndex = history.size() - 1;
        renderSpread(spread);
    }

    public void showPreviousImage() {
        if (historyIndex <= 0) return;
        historyIndex--;
        renderSpread(history.get(historyIndex));
    }

    public void showFirstImage() {
        if (imageIterator == null) return;
        resetNavigationState();
        addAndRenderSpreadFromAnchor(imageIterator.first());
    }

    public void showLastImage() {
        if (imageIterator == null) return;
        resetNavigationState();
        addAndRenderSpreadFromAnchor(imageIterator.last());
    }

    public void showNextVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        addAndRenderSpreadFromAnchor(imageIterator.nextVol());
    }

    public void showPrevVolume() {
        if (imageIterator == null) return;
        resetNavigationState();
        addAndRenderSpreadFromAnchor(imageIterator.prevVol());
    }

    public void processPicture(Picture picture) {
        resetNavigationState();
        addAndRenderSpreadFromAnchor(picture);
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
        if (twoPageMode) {
            tryPromoteCurrentSpreadToTwoPage();
        }
        rerenderCurrentSpread();
    }

    public void setComicsReadingMode() {
        readingMode = ReadingMode.COMICS;
        rerenderCurrentSpread();
    }

    public void setMangaReadingMode() {
        readingMode = ReadingMode.MANGA;
        rerenderCurrentSpread();
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

    private void addAndRenderSpreadFromAnchor(Picture anchor) {
        if (anchor == null) return;
        Spread spread = buildSpreadFromAnchor(anchor);
        if (spread == null) return;
        history.add(spread);
        historyIndex = history.size() - 1;
        renderSpread(spread);
    }

    private Spread createNextSpread() {
        Picture anchor = fetchNextPicture();
        return buildSpreadFromAnchor(anchor);
    }

    private Spread buildSpreadFromAnchor(Picture anchor) {
        if (anchor == null) return null;

        Picture second = null;
        if (shouldRenderTwoPages(anchor)) {
            Picture candidate = fetchNextPicture();
            if (canBePaired(anchor, candidate)) {
                second = candidate;
            } else if (candidate != null) {
                bufferedNext = candidate;
            }
        }

        return new Spread(anchor, second);
    }

    private boolean shouldRenderTwoPages(Picture anchor) {
        if (!twoPageMode || imageIterator == null || anchor == null) {
            return false;
        }
        return isPortrait(anchor.image());
    }

    private boolean canBePaired(Picture first, Picture second) {
        if (first == null || second == null) return false;
        return isPortrait(first.image()) && isPortrait(second.image());
    }

    private boolean isPortrait(BufferedImage image) {
        return image != null && image.getHeight() >= image.getWidth();
    }

    private Picture fetchNextPicture() {
        if (bufferedNext != null) {
            Picture picture = bufferedNext;
            bufferedNext = null;
            return picture;
        }
        if (imageIterator == null) return null;
        return imageIterator.next();
    }

    private void renderSpread(Spread spread) {
        if (spread == null || spread.anchor() == null) return;

        Picture anchor = spread.anchor();
        Picture second = twoPageMode ? spread.second() : null;

        PictureMetadata metadata = anchor.metadata();
        currentDir = metadata.dir();
        frame.setTitle(buildTitle(metadata, second));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));

        BufferedImage imageToRender = composeDisplayImage(anchor.image(), second == null ? null : second.image());
        imageComponent.setImageAndCenter(imageToRender, scrollPane);
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
        history.clear();
        historyIndex = -1;
        bufferedNext = null;
    }

    private void rerenderCurrentSpread() {
        if (historyIndex < 0 || historyIndex >= history.size()) return;
        renderSpread(history.get(historyIndex));
    }

    private void tryPromoteCurrentSpreadToTwoPage() {
        if (historyIndex < 0 || historyIndex >= history.size()) return;
        if (historyIndex != history.size() - 1) return;

        Spread current = history.get(historyIndex);
        if (current.second() != null) return;
        if (!isPortrait(current.anchor().image())) return;

        Picture candidate = fetchNextPicture();
        if (canBePaired(current.anchor(), candidate)) {
            history.set(historyIndex, new Spread(current.anchor(), candidate));
            return;
        }

        if (candidate != null) {
            bufferedNext = candidate;
        }
    }

    private enum ReadingMode {
        COMICS,
        MANGA
    }

    private record Spread(Picture anchor, Picture second) {
    }
}
