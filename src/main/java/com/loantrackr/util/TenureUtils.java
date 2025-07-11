package com.loantrackr.util;

import java.util.Arrays;
import java.util.List;

public class TenureUtils {

    public static List<Integer> parseSupportedTenures(String csvTenures) {
        if (csvTenures == null || csvTenures.isBlank()) return List.of();
        return Arrays.stream(csvTenures.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }

    public static boolean isTenureSupported(String csvTenures, int requestedTenure) {
        return parseSupportedTenures(csvTenures).contains(requestedTenure);
    }
}

