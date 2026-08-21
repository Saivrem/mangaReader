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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.dustyroom.be.models.Direction.NEXT;
import static org.dustyroom.be.models.Direction.PREV;
import static org.dustyroom.be.utils.FileUtils.isSupportedImage;
import static org.dustyroom.be.utils.FileUtils.isZipFile;
import static org.dustyroom.be.utils.NaturalOrderComparator.INSTANCE;

@Slf4j
public class ZipIterator implements ImageIterator {

    private List<PageRef> pageRefs = List.of();
    private File zipFilePath;
    private File pendingZipFilePath;
    private ZipFile zipFile;
    private int currentIndex = -1;
    private boolean initialSelectionPending;
    private boolean closed;

    public ZipIterator(File zipFilePath) {
        this.pendingZipFilePath = normalize(zipFilePath);
        init();
    }

    @Override
    public void init() {
        ensureNotClosed();
        File candidatePath = pendingZipFilePath != null ? pendingZipFilePath : zipFilePath;
        if (candidatePath == null) {
            throw new ImageIteratorException("ZIP source is not set", null);
        }

        ZipFile openedZip = null;
        try {
            openedZip = new ZipFile(candidatePath);
            List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> zipEntryEnumeration = openedZip.entries();

            while (zipEntryEnumeration.hasMoreElements()) {
                ZipEntry zipEntry = zipEntryEnumeration.nextElement();
                if (!zipEntry.isDirectory() && isSupportedImage(zipEntry.getName())) {
                    entries.add(zipEntry);
                }
            }

            entries.sort(Comparator.comparing(ZipEntry::getName, INSTANCE));
            if (entries.isEmpty()) {
                throw new ImageIteratorException("No supported images in ZIP: " + candidatePath.getName(), candidatePath);
            }

            ZipFile previousZip = zipFile;
            zipFile = openedZip;
            zipFilePath = candidatePath;
            pendingZipFilePath = null;
            pageRefs = entries.stream().map(entry -> pageRef(candidatePath, entry)).toList();
            currentIndex = 0;
            initialSelectionPending = true;
            closeQuietly(previousZip);
        } catch (ImageIteratorException e) {
            closeQuietly(openedZip);
            pendingZipFilePath = null;
            throw e;
        } catch (IOException e) {
            closeQuietly(openedZip);
            pendingZipFilePath = null;
            log.warn("Can't read the ZIP file {}", candidatePath, e);
            throw new ImageIteratorException("Can't open ZIP archive: " + candidatePath.getName(), candidatePath, e);
        }
    }

    @Override
    public File getVolumeRoot() {
        return zipFilePath;
    }

    @Override
    public void setFile(File file) {
        ensureNotClosed();
        this.pendingZipFilePath = normalize(file);
    }

    @Override
    public Picture next() {
        ensureOpen();
        if (initialSelectionPending) {
            initialSelectionPending = false;
        } else {
            currentIndex = Math.floorMod(currentIndex + 1, pageRefs.size());
        }
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture prev() {
        ensureOpen();
        initialSelectionPending = false;
        currentIndex = Math.floorMod(currentIndex - 1, pageRefs.size());
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture first() {
        ensureOpen();
        initialSelectionPending = false;
        currentIndex = 0;
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture last() {
        ensureOpen();
        initialSelectionPending = false;
        currentIndex = pageRefs.size() - 1;
        return load(pageRefs.get(currentIndex));
    }

    @Override
    public Picture load(PageRef page) {
        ensureOpen();
        String currentSourceId = sourceId(zipFilePath);
        if (!page.key().sourceId().equals(currentSourceId)) {
            throw new ImageIteratorException("Page does not belong to the current ZIP source", zipFilePath);
        }
        ZipEntry entry = zipFile.getEntry(page.key().pageId());
        if (entry == null || entry.isDirectory()) {
            throw new ImageIteratorException("ZIP entry no longer exists: " + page.key().pageId(), zipFilePath);
        }
        log.debug("File: {}", entry.getName());
        try (InputStream input = zipFile.getInputStream(entry)) {
            BufferedImage read = ImageDecoder.read(input);
            if (read == null) {
                throw new ImageIteratorException(
                        "Unsupported or corrupt image in ZIP: " + entry.getName(),
                        zipFilePath
                );
            }
            return new Picture(read, page.metadata());
        } catch (IOException e) {
            log.warn("Can't read image {} from {}", entry.getName(), zipFilePath, e);
            throw new ImageIteratorException(
                    "Can't read image " + entry.getName() + " from " + zipFilePath.getName(),
                    zipFilePath,
                    e
            );
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
        return 0;
    }

    @Override
    public PageRef switchToNextVolume() {
        ensureCurrentVolumeAvailable();
        return switchVolume(isZipFile, NEXT);
    }

    @Override
    public PageRef switchToPreviousVolume() {
        ensureCurrentVolumeAvailable();
        return switchVolume(isZipFile, PREV);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(zipFile);
        zipFile = null;
        pendingZipFilePath = null;
        pageRefs = List.of();
        currentIndex = -1;
        initialSelectionPending = false;
    }

    private void ensureOpen() {
        if (closed || zipFile == null) {
            throw new ImageIteratorException("ZIP archive is closed", zipFilePath);
        }
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new ImageIteratorException("ZIP archive is closed", zipFilePath);
        }
    }

    private void ensureCurrentVolumeAvailable() {
        ensureOpen();
        if (!isZipFile.test(zipFilePath)) {
            throw new ImageIteratorException("Current ZIP source is no longer available", zipFilePath);
        }
    }

    private void closeQuietly(ZipFile source) {
        if (source == null) {
            return;
        }
        try {
            source.close();
        } catch (IOException e) {
            log.warn("Can't close the ZIP file {}", zipFilePath, e);
        }
    }

    private PageRef pageRef(File source, ZipEntry entry) {
        return new PageRef(
                new PageKey(sourceId(source), entry.getName()),
                new PictureMetadata(entry.getName(), source.getName(), source.getParentFile())
        );
    }

    private String sourceId(File source) {
        return source == null ? "" : source.toPath().toAbsolutePath().normalize().toString();
    }

    private File normalize(File source) {
        return source == null ? null : source.toPath().toAbsolutePath().normalize().toFile();
    }

    private Picture consumeVolumeSelection(PageRef page) {
        if (page == null) {
            return null;
        }
        int selectedIndex = pageRefs.indexOf(page);
        if (selectedIndex < 0) {
            throw new ImageIteratorException("Selected page is not part of the current ZIP source", zipFilePath);
        }
        currentIndex = selectedIndex;
        initialSelectionPending = false;
        return load(page);
    }
}
