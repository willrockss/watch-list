package io.kluev.watchlist.common.utils;

import org.apache.commons.lang3.StringUtils;

abstract public class NumberUtils {

    public static Integer parseOrNull(String input) {
        if (input == null) {
            return null;
        }

        if (!StringUtils.isNumeric(input)) {
            return null;
        }

        try {
            return Integer.valueOf(input);
        } catch (Exception e) {
            return null;
        }
    }
}
