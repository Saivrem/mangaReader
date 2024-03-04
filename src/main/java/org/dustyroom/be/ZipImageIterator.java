package org.dustyroom.be;

import lombok.extern.slf4j.Slf4j;

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
            current = listIterator.next();
            listIterator.previous();
        } catch (Exception e) {
            log.error("Can't read Zip file {}\n application will be closed", zipFilePath);
            System.exit(1);
        }
    }

    private BufferedImage readImageFromZip(ZipEntry entry) {
        System.out.println("File: " + entry.getName());
        current = entry;
        try {
            return ImageIO.read(zipFile.getInputStream(entry));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public BufferedImage next() {
        ZipEntry next;
        if (listIterator.hasNext()) {
            next = listIterator.next();
            if (current.equals(next)) {
                next = listIterator.next();
            }
        } else {
            listIterator = entryList.listIterator(1);
            next = listIterator.previous();
        }
        return readImageFromZip(next);
    }

    @Override
    public BufferedImage prev() {
        ZipEntry previous;
        if (listIterator.hasPrevious()) {
            previous = listIterator.previous();
            if (current.equals(previous)) {
                previous = listIterator.previous();
            }
        } else {
            listIterator = entryList.listIterator(listSize - 1);
            previous = listIterator.next();
        }
        return readImageFromZip(previous);
    }
}
