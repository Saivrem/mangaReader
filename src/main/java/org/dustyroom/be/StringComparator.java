package org.dustyroom.be;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

@Slf4j
public class StringComparator implements Comparator<String> {

    public static boolean hasOnlyNumbers(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }

        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int compare(String name1, String name2) {
        String modifiedName1 = name1.substring(0, name1.lastIndexOf("."));
        String modifiedName2 = name2.substring(0, name2.lastIndexOf("."));

        // TODO extract numeric part and compare as numbers, or else as strings
        // For numeric only strings this will make no difference
        // Won't produce collisions for sorting of numeric only vs alpha-numeric
        if (hasOnlyNumbers(modifiedName1) && hasOnlyNumbers(modifiedName2)) {
            long numericName1 = Long.parseLong(modifiedName1);
            long numericName2 = Long.parseLong(modifiedName2);

            return Long.compare(numericName1, numericName2);
        }

        return String.CASE_INSENSITIVE_ORDER.compare(modifiedName1, modifiedName2);
    }

    @Deprecated
    private Integer extractNumericPart(String path) {
        StringBuilder numeric = new StringBuilder();

        for (char ch : path.toCharArray()) {
            if (Character.isDigit(ch)) {
                numeric.append(ch);
            }
        }
        try {
            return Integer.parseInt(numeric.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
