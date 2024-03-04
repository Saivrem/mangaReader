package org.dustyroom.be.iterators;

import lombok.extern.slf4j.Slf4j;
import org.dustyroom.be.models.Picture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class ZipImageIterator implements ImageIterator {

    private final List<ZipEntry> entryList = new ArrayList<>();
    private ZipFile zipFile;
    private int listSize;
    private ListIterator<ZipEntry> listIterator;
    private ZipEntry current;

    public ZipImageIterator(File zipFilePath) {
        try {
            zipFile = new ZipFile(zipFilePath);
            Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();

            while (zipEntryEnumeration.hasMoreElements()) {
                entryList.add(zipEntryEnumeration.nextElement());
            }

            listSize = entryList.size();
            listIterator = entryList.listIterator();
        } catch (Exception e) {
            log.error("Can't read Zip file {}\n application will be closed", zipFilePath);
            System.exit(1);
        }
    }

    @Override
    public Picture next() {
        ZipEntry next;
        if (listIterator.hasNext()) {
            next = listIterator.next();
            if (next.equals(current)) {
                next = listIterator.next();
            }
        } else {
            return first();
        }
        return readImageFrom(next);
    }

    @Override
    public Picture prev() {
        ZipEntry prev;
        if (listIterator.hasPrevious()) {
            prev = listIterator.previous();
            if (prev.equals(current)) {
                prev = listIterator.previous();
            }
        } else {
            return last();
        }
        return readImageFrom(prev);
    }

    @Override
    public Picture first() {
        listIterator = entryList.listIterator(1);
        return readImageFrom(listIterator.previous());
    }

    @Override
    public Picture last() {
        listIterator = entryList.listIterator(listSize - 1);
        return readImageFrom(listIterator.next());
    }

    private Picture readImageFrom(ZipEntry entry) {
        log.debug("File: {}", entry.getName());
        current = entry;
        try {
            BufferedImage read = ImageIO.read(zipFile.getInputStream(entry));
            return new Picture(entry.getName(), read);
        } catch (IOException e) {
            return null;
        }
    }
}
