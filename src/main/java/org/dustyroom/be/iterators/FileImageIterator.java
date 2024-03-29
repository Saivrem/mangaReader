package org.dustyroom.be.iterators;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.models.Picture;
import org.dustyroom.be.models.PictureMetadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.dustyroom.be.utils.FileUtils.isSupported;

@Slf4j
public class FileImageIterator implements ImageIterator {
    private final List<File> fileList = new ArrayList<>();
    private int currentIndex;
    private ListIterator<File> listIterator;
    private int listSize;

    public FileImageIterator(File file) {
        File parent = file.getParentFile();
        try {
            for (File f : Objects.requireNonNull(parent.listFiles())) {
                if (isSupported(f.getName())) {
                    fileList.add(f);
                }
            }
            listSize = fileList.size();
        } catch (Exception e) {
            log.error("Can't list files in {}", parent);
            System.exit(1);
        }

        fileList.sort(Comparator.comparing(File::getName, stringComparator));
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
                next = listIterator.next();
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
        listIterator = fileList.listIterator(1);
        return readImageFrom(listIterator.previous());
    }

    @Override
    public Picture last() {
        // TODO Think about code duplication
        listIterator = fileList.listIterator(listSize - 1);
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
}
