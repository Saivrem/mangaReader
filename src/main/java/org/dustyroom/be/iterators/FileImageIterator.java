package org.dustyroom.be.iterators;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.models.PageKey;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static org.dustyroom.be.models.Direction.NEXT;
import static org.dustyroom.be.models.Direction.PREV;
import static org.dustyroom.be.utils.FileUtils.*;

@Slf4j
public class FileImageIterator implements ImageIterator {
    private List<File> fileList = List.of();
    private List<PageRef> pageRefs = List.of();
    private File volumeRoot;
    private File pendingFile;
    private int currentIndex = -1;
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
            File candidateVolumeRoot;
            List<File> candidateFiles;
            int candidateIndex;

            if (isImage.test(candidate)) {
                candidateVolumeRoot = candidate.getParentFile();
                candidateFiles = getSortedFiles(candidateVolumeRoot, isImage);
                int selectedIndex = candidateFiles.indexOf(candidate);
                candidateIndex = Math.max(selectedIndex, 0);
            } else if (containsImages.test(candidate)) {
                candidateVolumeRoot = candidate;
                candidateFiles = getSortedFiles(candidate, isImage);
                candidateIndex = 0;
            } else {
                candidateVolumeRoot = null;
                candidateFiles = List.of();
                candidateIndex = -1;
            }

            if (candidateFiles.isEmpty()) {
                throw new ImageIteratorException("No supported images found", candidate);
            }

            List<PageRef> candidatePageRefs = candidateFiles.stream().map(this::pageRef).toList();
            volumeRoot = candidateVolumeRoot;
            fileList = candidateFiles;
            pageRefs = candidatePageRefs;
            currentIndex = candidateIndex;
            initialPageIndex = candidateIndex;
            initialSelectionPending = true;
        } finally {
            pendingFile = null;
        }
    }

    @Override
    public File getVolumeRoot() {
        return volumeRoot;
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
    public Picture load(PageRef page) {
        File imageFile = new File(page.key().pageId());
        String expectedSource = volumeId(getVolumeRoot());
        if (!page.key().sourceId().equals(expectedSource)) {
            throw new ImageIteratorException("Page does not belong to the current image source", imageFile);
        }
        try (InputStream input = Files.newInputStream(imageFile.toPath())) {
            BufferedImage read = ImageDecoder.read(input);
            if (read == null) {
                throw new ImageIteratorException("Unsupported or corrupt image: " + imageFile.getName(), imageFile);
            }
            return new Picture(read, page.metadata());
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
    public int initialPageIndex() {
        return initialPageIndex;
    }

    @Override
    public PageRef switchToNextVolume() {
        return switchVolume(containsImages, NEXT);
    }

    @Override
    public PageRef switchToPreviousVolume() {
        return switchVolume(containsImages, PREV);
    }

    private PageRef pageRef(File imageFile) {
        File directory = imageFile.getParentFile();
        return new PageRef(
                new PageKey(volumeId(directory), imageFile.toPath().toAbsolutePath().normalize().toString()),
                new PictureMetadata(imageFile.getName(), directory.getName(), directory)
        );
    }

    private String volumeId(File directory) {
        return directory == null ? "" : directory.toPath().toAbsolutePath().normalize().toString();
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

    private File normalize(File source) {
        return source == null ? null : source.toPath().toAbsolutePath().normalize().toFile();
    }
}
