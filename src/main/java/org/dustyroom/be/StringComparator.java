package org.dustyroom.be;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class StringComparator implements Comparator<String> {

    @Override
    public int compare(String name1, String name2) {
        name1 = name1.substring(0, name1.lastIndexOf("."));
        name2 = name2.substring(0, name2.lastIndexOf("."));

        if (hasOnlyNumbers(name1) && hasOnlyNumbers(name2)) {
            long numericName1 = Long.parseLong(name1);
            long numericName2 = Long.parseLong(name2);
            return Long.compare(numericName1, numericName2);
        }

        if (containsDigits(name1) && containsDigits(name2)) {
            Integer first = extractNumericPart(name1);
            Integer second = extractNumericPart(name2);
            if (first != null && second != null) {
                return Integer.compare(first, second);
            }
        }

        return name1.compareTo(name2);
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
