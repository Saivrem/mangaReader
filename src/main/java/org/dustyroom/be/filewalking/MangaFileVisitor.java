package org.dustyroom.be.filewalking;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MangaFileVisitor extends SimpleFileVisitor<Path> {

    @Getter
    private final Map<Path, List<Path>> tree = new TreeMap<>();
    private final List<String> blacklist = List.of(".DS_Store");

    private List<Path> files = new ArrayList<>();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (!blacklisted(file.toString())) {
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
        files.sort(Path::compareTo);
        tree.put(dir, files);
        files = new ArrayList<>();
        return FileVisitResult.CONTINUE;
    }

    private boolean blacklisted(String string) {
        return blacklist.stream().anyMatch(string::contains);
    }
}
