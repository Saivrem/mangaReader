package org.dustyroom.be.filewalking;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.dustyroom.be.utils.PathUtils.isImage;

public class MangaFileVisitor extends SimpleFileVisitor<Path> {
    private final static PathComparator pathComparator = new PathComparator();

    @Getter
    private final Map<Path, List<Path>> tree = new TreeMap<>();

    private List<Path> files = new ArrayList<>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (isImage(file)) {
            files.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        files.sort(pathComparator);
        tree.put(dir, files);
        files = new ArrayList<>();
        return FileVisitResult.CONTINUE;
    }
}
