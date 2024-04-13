package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Picture;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static org.dustyroom.be.utils.FileUtils.getSortedFilesFromParent;
import static org.dustyroom.be.utils.ListUtils.getFirstOrDefault;
import static org.dustyroom.be.utils.ListUtils.getLastOrDefault;

public interface ImageIterator {
    void init();

    File getVolumeRoot();

    void setFile(File file);

    Picture next();

    Picture prev();

    Picture first();

    Picture last();

    Picture nextVol();

    Picture prevVol();

    default Picture nextVol(Predicate<File> predicate) {
        File filePath = getVolumeRoot();
        List<File> sortedFiles = getSortedFilesFromParent(filePath, predicate);
        for (int i = 0; i < sortedFiles.size(); i++) {
            if (sortedFiles.get(i).equals(filePath)) {
                if (i == sortedFiles.size() - 1) {
                    setFile(getFirstOrDefault(sortedFiles, filePath));
                } else {
                    setFile(sortedFiles.get(i + 1));
                }
                break;
            }
        }
        if (sortedFiles.size() > 1) {
            init();
        }
        return next();
    }

    default Picture prevVol(Predicate<File> predicate) {
        File filePath = getVolumeRoot();

        List<File> sortedFiles = getSortedFilesFromParent(filePath, predicate);
        for (int i = 0; i < sortedFiles.size(); i++) {
            if (sortedFiles.get(i).equals(filePath)) {
                if (i == 0) {
                    setFile(getLastOrDefault(sortedFiles, filePath));
                } else {
                    setFile(sortedFiles.get(i - 1));
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
