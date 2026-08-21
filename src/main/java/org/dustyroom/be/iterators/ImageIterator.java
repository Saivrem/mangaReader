package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Direction;
import org.dustyroom.be.models.PageRef;
import org.dustyroom.be.models.Picture;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static org.dustyroom.be.utils.FileUtils.getSortedFilesFromParent;

public interface ImageIterator extends AutoCloseable {
    void init();

    File getVolumeRoot();

    void setFile(File file);

    Picture next();

    Picture prev();

    Picture first();

    Picture last();

    Picture nextVol();

    Picture prevVol();

    List<PageRef> pages();

    int initialPageIndex();

    Picture load(PageRef page);

    PageRef switchToNextVolume();

    PageRef switchToPreviousVolume();

    @Override
    default void close() {
    }

    default PageRef switchVolume(Predicate<File> fileType, Direction dir) {
        File filePath = getVolumeRoot();
        List<File> sortedFiles = getSortedFilesFromParent(filePath, fileType);
        if (sortedFiles.isEmpty()) {
            if (filePath == null || !fileType.test(filePath)) {
                throw new ImageIteratorException("Current volume no longer exists", filePath);
            }
            return null;
        }

        int currentIndex = sortedFiles.indexOf(filePath);
        if (currentIndex < 0) {
            throw new ImageIteratorException("Current volume no longer exists", filePath);
        }
        int targetIndex = Math.floorMod(currentIndex + dir.getShift(), sortedFiles.size());
        setFile(sortedFiles.get(targetIndex));
        init();
        List<PageRef> pages = pages();
        return pages.isEmpty() ? null : pages.get(initialPageIndex());
    }
}
