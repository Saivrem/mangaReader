package org.dustyroom.be.iterators;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static org.dustyroom.be.utils.FileUtils.*;
import static org.dustyroom.be.utils.ListUtils.getFirstOrDefault;
import static org.dustyroom.be.utils.ListUtils.getLastOrDefault;

@Slf4j
public class FileImageIterator implements ImageIterator {
    private List<File> fileList;
    private File file;
    private int currentIndex;
    private ListIterator<File> listIterator;
    private int listSize;

    public FileImageIterator(File file) {
        this.file = file;
        init();
    }

    private void init() {
        if (isImage.test(file)) {
            fileList = getSortedFilesFromParent(file, isImage);
        } else if (isNotEmptyDirectory.test(file)) {
            fileList = getSortedFiles(file, isImage);
            file = getFirstOrDefault(fileList, file);
        } else {
            fileList = new ArrayList<>();
        }
        listSize = fileList.size();
        currentIndex = fileList.indexOf(file);
        listIterator = fileList.listIterator(currentIndex);
        if (currentIndex == 0) {
            prev();
        }
    }

    @Override
    public Picture next() {
        File next;
        if (listIterator.hasNext()) {
            next = listIterator.next();
            if (isSameFile(next)) {
                if (listIterator.hasNext()) {
                    next = listIterator.next();
                } else {
                    return first();
                }
            }
        } else {
            return first();
        }
        currentIndex++;
        return readImageFrom(next);
    }

    @Override
    public Picture prev() {
        File prev;
        if (listIterator.hasPrevious()) {
            prev = listIterator.previous();
            if (isSameFile(prev)) {
                prev = listIterator.previous();
            }
        } else {
            return last();
        }
        currentIndex--;
        return readImageFrom(prev);
    }

    private boolean isSameFile(File file) {
        return currentIndex == fileList.indexOf(file);
    }

    @Override
    public Picture first() {
        currentIndex = 1;
        listIterator = fileList.listIterator(currentIndex);
        return prev();
    }

    @Override
    public Picture last() {
        // TODO Think about code duplication
        currentIndex = listSize - 1;
        listIterator = fileList.listIterator(currentIndex);
        return readImageFrom(listIterator.next());
    }

    private Picture readImageFrom(File file) {
        currentIndex = fileList.indexOf(file);
        try {
            BufferedImage read = ImageIO.read(Files.newInputStream(file.toPath()));
            return new Picture(
                    read,
                    new PictureMetadata(file.getName(), file.getParentFile())
            );
        } catch (IOException e) {
            log.warn("Can't read the file {}", file);
            return null;
        }
    }

    @Override
    public Picture nextVol() {
        File parentFile = file.getParentFile();
        List<File> sortedFiles = getSortedFilesFromParent(parentFile, isNotEmptyDirectory);
        for (int i = 0; i < sortedFiles.size(); i++) {
            if (sortedFiles.get(i).equals(parentFile)) {
                if (i == sortedFiles.size() - 1) {
                    file = getFirstOrDefault(sortedFiles, file);
                } else {
                    file = sortedFiles.get(i + 1);
                }
                break;
            }
        }
        if (sortedFiles.size() > 1) {
            init();
        }
        return next();
    }

    @Override
    public Picture prevVol() {
        File parentFile = file.getParentFile();
        List<File> sortedFiles = getSortedFilesFromParent(parentFile, isNotEmptyDirectory);
        for (int i = 0; i < sortedFiles.size(); i++) {
            if (sortedFiles.get(i).equals(parentFile)) {
                if (i == 0) {
                    file = getLastOrDefault(sortedFiles, file);
                } else {
                    file = sortedFiles.get(i - 1);
                }
                break;
            }
        }
        if (sortedFiles.size() > 1) {
            init();
        }
        return next();
    }
}
