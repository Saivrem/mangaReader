package org.dustyroom.be.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalOrderComparatorTest {

    private static final NaturalOrderComparator C = NaturalOrderComparator.INSTANCE;

    @Test
    void sortsPureNumbersNaturally() {
        List<String> input = new ArrayList<>(Arrays.asList("10", "2", "1", "20", "3"));
        input.sort(C);
        assertEquals(Arrays.asList("1", "2", "3", "10", "20"), input);
    }

    @Test
    void sortsAlphaNumericNaturally() {
        List<String> input = new ArrayList<>(Arrays.asList(
                "ch2_page10.png",
                "ch2_page2.png",
                "ch10_page1.png",
                "ch2_page1.png"
        ));
        input.sort(C);
        assertEquals(Arrays.asList(
                "ch2_page1.png",
                "ch2_page2.png",
                "ch2_page10.png",
                "ch10_page1.png"
        ), input);
    }

    @Test
    void treatsLeadingZeroesAsSmallerWhenNumbersEqual() {
        List<String> input = new ArrayList<>(Arrays.asList("1", "001", "01", "0001"));
        input.sort(C);
        assertEquals(Arrays.asList("0001", "001", "01", "1"), input);
    }

    @Test
    void comparesNumericTokenByValueNotLexicographically() {
        assertTrue(C.compare("file9", "file10") < 0);
        assertTrue(C.compare("file10", "file100") < 0);
    }

    @Test
    void fallsBackToCaseSensitiveOrderWhenCaseInsensitiveEqual() {
        // "A" < "a" in ASCII/Unicode ordering
        assertTrue(C.compare("A", "a") < 0);
    }
}
