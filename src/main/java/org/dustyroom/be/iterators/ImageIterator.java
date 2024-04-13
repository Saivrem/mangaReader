package org.dustyroom.be.iterators;

import org.dustyroom.be.models.Direction;
import org.dustyroom.be.models.Picture;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

import static org.dustyroom.be.models.Direction.NEXT;
import static org.dustyroom.be.models.Direction.PREV;
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

    default Picture switchVol(Predicate<File> fileType, Direction dir) {
        File filePath = getVolumeRoot();
        List<File> sortedFiles = getSortedFilesFromParent(filePath, fileType);
        if (!sortedFiles.isEmpty()) {
            int i = sortedFiles.indexOf(filePath);
            int last = sortedFiles.size() - 1;
            if (i == last && dir == NEXT) {
                setFile(getFirstOrDefault(sortedFiles, filePath));
            } else if (i == 0 && dir == PREV) {
                setFile(getLastOrDefault(sortedFiles, filePath));
            } else {
                setFile(sortedFiles.get(i + dir.getShift()));
            }
            init();
        }

        return next();
    }
}
