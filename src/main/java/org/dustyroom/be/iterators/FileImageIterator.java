package org.dustyroom.be.iterators;

import lombok.Getter;
import lombok.experimental.Accessors;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.animation.gif.GifDecoder;
import org.dustyroom.be.models.animation.AnimationClip;
import org.dustyroom.be.models.book.PageKey;
import org.dustyroom.be.models.book.PageRef;
import org.dustyroom.be.models.book.Picture;
import org.dustyroom.be.models.book.PictureMetadata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.dustyroom.be.models.direction.Direction.NEXT;
import static org.dustyroom.be.models.direction.Direction.PREV;
import static org.dustyroom.be.utils.FileUtils.*;

@Slf4j
public class FileImageIterator implements ImageIterator {
    private List<File> fileList = List.of();
    private List<PageRef> pageRefs = List.of();
    @Getter
    private File volumeRoot;
    private File pendingFile;
    private int currentIndex = -1;
    @Getter
    @Accessors(fluent = true)
    private int initialPageIndex = -1;
    private boolean initialSelectionPending;

    public FileImageIterator(File file) {
        this.pendingFile = normalize(file);
        init();
    }

    @Override
    public void init() {
        File candidate = pendingFile != null ? pendingFile : currentFile();
        try {
            boolean singleImage = isImage.test(candidate);
            File root = singleImage ? candidate.getParentFile() : candidate;
            List<File> files = getSortedFiles(root, isImage);
            if (files.isEmpty()) {
                throw new ImageIteratorException("No supported images found", candidate);
            }

            int candidateIndex = singleImage ? Math.max(files.indexOf(candidate), 0) : 0;
            String sourceId = root.getPath();
            List<PageRef> pages = files
                    .stream()
                    .map(image -> pageRef(root, sourceId, image))
                    .toList();

            volumeRoot = root;
            fileList = files;
            pageRefs = pages;
            currentIndex = candidateIndex;
            initialPageIndex = candidateIndex;
            initialSelectionPending = true;
        } finally {
            pendingFile = null;
        }
    }

    @Override
    public void setFile(File file) {
        this.pendingFile = normalize(file);
    }

    @Override
    public Picture next() {
        if (initialSelectionPending) {
            initialSelectionPending = false;
        } else {
            currentIndex = Math.floorMod(currentIndex + 1, fileList.size());
        }
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture prev() {
        initialSelectionPending = false;
        currentIndex = Math.floorMod(currentIndex - 1, fileList.size());
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture first() {
        initialSelectionPending = false;
        currentIndex = 0;
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture last() {
        initialSelectionPending = false;
        currentIndex = fileList.size() - 1;
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public AnimationClip animation(PageRef page) {
        if (!page.getKey().sourceId().equals(normalizedPath(getVolumeRoot()))) {
            throw new IllegalArgumentException("Wrong source");
        }
        Path path = Path.of(page.getKey().pageId());
        return GifDecoder.read(() -> Files.newInputStream(path));
    }

    @Override
    public Picture load(PageRef page) {
        File imageFile = new File(page.getKey().pageId());
        String expectedSource = normalizedPath(getVolumeRoot());
        if (!page.getKey().sourceId().equals(expectedSource)) {
            throw new ImageIteratorException("Page does not belong to the current image source", imageFile);
        }
        try (InputStream input = Files.newInputStream(imageFile.toPath())) {
            BufferedImage read = ImageDecoder.read(input);
            if (read == null) {
                throw new ImageIteratorException("Unsupported or corrupt image: " + imageFile.getName(), imageFile);
            }
            return new Picture(read, page.getMetadata());
        } catch (IOException e) {
            log.warn("Can't read the file {}", imageFile, e);
            throw new ImageIteratorException("Can't read image: " + imageFile.getName(), imageFile, e);
        }
    }

    @Override
    public Picture nextVol() {
        PageRef page = switchToNextVolume();
        return consumeVolumeSelection(page);
    }

    @Override
    public Picture prevVol() {
        PageRef page = switchToPreviousVolume();
        return consumeVolumeSelection(page);
    }

    @Override
    public List<PageRef> pages() {
        return pageRefs;
    }

    @Override
    public PageRef switchToNextVolume() {
        return switchVolume(containsImages, NEXT);
    }

    @Override
    public PageRef switchToPreviousVolume() {
        return switchVolume(containsImages, PREV);
    }

    private PageRef pageRef(File directory, String sourceId, File imageFile) {
        return new PageRef(
                new PageKey(sourceId, imageFile.getPath()),
                new PictureMetadata(imageFile.getName(), directory.getName(), directory)
        );
    }

    private Picture consumeVolumeSelection(PageRef page) {
        if (page == null) {
            return null;
        }
        int selectedIndex = pageRefs.indexOf(page);
        if (selectedIndex < 0) {
            throw new ImageIteratorException("Selected page is not part of the current image source", volumeRoot);
        }
        currentIndex = selectedIndex;
        initialSelectionPending = false;
        return load(page);
    }

    private File currentFile() {
        if (currentIndex >= 0 && currentIndex < fileList.size()) {
            return fileList.get(currentIndex);
        }
        return volumeRoot;
    }
}
