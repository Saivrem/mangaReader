package org.dustyroom.be.utils;

import java.util.Comparator;

/**
 * Natural order comparator for strings.
 * Splits strings into digit and non-digit tokens and compares them piecewise.
 * Numeric tokens are compared by numeric value (without parsing to number),
 * and when values are equal, tokens with more leading zeros are ordered first.
 */
public final class NaturalOrderComparator implements Comparator<String> {

    public static final NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

    @Override
    public int compare(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        int leftIndex = 0;
        int rightIndex = 0;
        int leftLength = a.length();
        int rightLength = b.length();

        while (leftIndex < leftLength || rightIndex < rightLength) {
            if (leftIndex >= leftLength) return -1;
            if (rightIndex >= rightLength) return 1;

            char leftChar = a.charAt(leftIndex);
            char rightChar = b.charAt(rightIndex);
            boolean leftIsDigit = Character.isDigit(leftChar);
            boolean rightIsDigit = Character.isDigit(rightChar);

            if (leftIsDigit && rightIsDigit) {
                int leftTokenStart = leftIndex;
                int rightTokenStart = rightIndex;
                while (leftIndex < leftLength && Character.isDigit(a.charAt(leftIndex))) leftIndex++;
                while (rightIndex < rightLength && Character.isDigit(b.charAt(rightIndex))) rightIndex++;

                int cmp = compareNumericToken(a, leftTokenStart, leftIndex, b, rightTokenStart, rightIndex);
                if (cmp != 0) return cmp;
            } else {
                int leftTokenStart = leftIndex;
                int rightTokenStart = rightIndex;
                while (leftIndex < leftLength && !Character.isDigit(a.charAt(leftIndex))) leftIndex++;
                while (rightIndex < rightLength && !Character.isDigit(b.charAt(rightIndex))) rightIndex++;

                String leftToken = a.substring(leftTokenStart, leftIndex);
                String rightToken = b.substring(rightTokenStart, rightIndex);

                int cmp = leftToken.compareToIgnoreCase(rightToken);
                if (cmp != 0) return cmp;
                cmp = leftToken.compareTo(rightToken);
                if (cmp != 0) return cmp;
            }
        }

        return 0;
    }

    private int compareNumericToken(String left, int leftStart, int leftEnd, String right, int rightStart, int rightEnd) {
        int leftTokenLength = leftEnd - leftStart;
        int rightTokenLength = rightEnd - rightStart;

        int leftFirstNonZero = leftStart;
        int rightFirstNonZero = rightStart;
        while (leftFirstNonZero < leftEnd && left.charAt(leftFirstNonZero) == '0') leftFirstNonZero++;
        while (rightFirstNonZero < rightEnd && right.charAt(rightFirstNonZero) == '0') rightFirstNonZero++;

        int leftSignificantLength = leftEnd - leftFirstNonZero;
        int rightSignificantLength = rightEnd - rightFirstNonZero;

        // Compare by numeric value (length of non-zero part)
        if (leftSignificantLength != rightSignificantLength) {
            return Integer.compare(leftSignificantLength, rightSignificantLength);
        }

        // Both are all zeros => equal numeric value
        if (leftSignificantLength == 0) return 0;

        // Same length numeric value, compare lexicographically
        for (int i = 0; i < leftSignificantLength; i++) {
            char leftChar = left.charAt(leftFirstNonZero + i);
            char rightChar = right.charAt(rightFirstNonZero + i);
            if (leftChar != rightChar) return Character.compare(leftChar, rightChar);
        }

        // Numeric values equal, order by number of leading zeros (more zeros first)
        if (leftTokenLength != rightTokenLength) {
            return Integer.compare(rightTokenLength, leftTokenLength);
        }

        return 0;
    }
}
