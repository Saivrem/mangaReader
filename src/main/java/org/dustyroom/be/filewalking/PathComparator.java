package org.dustyroom.be.filewalking;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
public class PathComparator implements Comparator<Path> {
    @Override
    public int compare(Path path1, Path path2) {
        String firstFileName = path1.getFileName().toString();
        firstFileName = firstFileName.substring(0, firstFileName.lastIndexOf("."));
        String secondFileName = path2.getFileName().toString();
        secondFileName = secondFileName.substring(0, secondFileName.lastIndexOf("."));

        if (hasOnlyNumbers(firstFileName) && hasOnlyNumbers(secondFileName)) {
            return path1.compareTo(path2);
        }

        if (containsDigits(firstFileName) && containsDigits(secondFileName)) {
            Integer first = extractNumericPart(firstFileName);
            Integer second = extractNumericPart(secondFileName);
            if (first != null && second != null) {
                return Integer.compare(first, second);
            }
        }

        return path1.compareTo(path2);
    }

    public static boolean hasOnlyNumbers(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    private Integer extractNumericPart(String path) {
        String[] parts = path.split("\\D+");
        StringBuilder numericStringBuilder = new StringBuilder();
        for (String part : parts) {
            if (part.matches("\\d+")) {
                numericStringBuilder.append(part);
            }
        }
        try {
            return Integer.parseInt(numericStringBuilder.toString());
        } catch (NumberFormatException e) {
            log.warn("Can't extract numeric part from {}", path);
            return null;
        }
    }

    private boolean containsDigits(String input) {
        return input.matches(".*\\d.*");
    }
}

