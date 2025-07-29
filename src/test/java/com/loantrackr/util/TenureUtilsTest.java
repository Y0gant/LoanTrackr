package com.loantrackr.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TenureUtilsTest {

    @Test
    void testParseSupportedTenures_NullInput() {
        List<Integer> result = TenureUtils.parseSupportedTenures(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseSupportedTenures_BlankInput() {
        List<Integer> result = TenureUtils.parseSupportedTenures("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseSupportedTenures_SingleTenure() {
        List<Integer> result = TenureUtils.parseSupportedTenures("12");
        assertEquals(List.of(12), result);
    }

    @Test
    void testParseSupportedTenures_MultipleTenures() {
        List<Integer> result = TenureUtils.parseSupportedTenures("6,12, 24");
        assertEquals(List.of(6, 12, 24), result);
    }

    @Test
    void testParseSupportedTenures_InvalidTenure_ThrowsException() {
        assertThrows(NumberFormatException.class, () ->
                TenureUtils.parseSupportedTenures("6,abc,12")
        );
    }

    @Test
    void testIsTenureSupported_TenurePresent() {
        boolean result = TenureUtils.isTenureSupported("6,12,24", 12);
        assertTrue(result);
    }

    @Test
    void testIsTenureSupported_TenureNotPresent() {
        boolean result = TenureUtils.isTenureSupported("6,12,24", 18);
        assertFalse(result);
    }

    @Test
    void testIsTenureSupported_NullCsv() {
        boolean result = TenureUtils.isTenureSupported(null, 6);
        assertFalse(result);
    }

    @Test
    void testIsTenureSupported_BlankCsv() {
        boolean result = TenureUtils.isTenureSupported("   ", 6);
        assertFalse(result);
    }

    @Test
    void testIsTenureSupported_InvalidTenureInCsv() {
        assertThrows(NumberFormatException.class, () ->
                TenureUtils.isTenureSupported("12,x,24", 12)
        );
    }
}
