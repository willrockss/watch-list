package io.kluev.watchlist.infra.jackett;

import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.io.FilenameUtils.getExtension;

public abstract class WLFilenameUtils {
    public static final int MAX_FILENAME_LENGTH = 170;

    /**
     * Remove all inappropriate symbols for Unix filename
     */
    public static String escapeFilename(String initialFilename) {
        val escapedFull = initialFilename
                .replaceAll("[^.\\d\\p{L}\\p{M}()\\[\\]{}:]", "_")
                .replaceAll("_+", "_");

        val ext = getExtWithDot(escapedFull);
        val escapedBase = FilenameUtils.getBaseName(escapedFull);
        return fit(escapedBase, MAX_FILENAME_LENGTH - ext.length()) + ext;
    }

    private static String getExtWithDot(String filename) {
        val ext = getExtension(filename);
        return StringUtils.isBlank(ext) ? "" : "." + ext;
    }

    public static String fit(String str, int maxLength) {
        if (str.length() > maxLength) {
            return str.substring(0, maxLength);
        }
        return str;
    }
}
