// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.utils;

import java.util.Locale;

/**
 * Utility class providing static formatting helpers for data sizes, counts, and strings.
 */
public final class FormatUtils {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final long TB = GB * 1024L;

    private FormatUtils() {
    }

    /**
     * Formats raw byte size into a clean human-readable representation:
     * - 0 B
     * - 850 B
     * - 42.5 KB
     * - 48.2 MB
     * - 1.42 GB
     * - 2.10 TB
     *
     * @param bytes the byte count to format
     * @return formatted size string with appropriate unit
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        if (bytes < KB) {
            return bytes + " B";
        }
        if (bytes < MB) {
            return String.format(Locale.ROOT, "%.1f KB", (double) bytes / KB);
        }
        if (bytes < GB) {
            return String.format(Locale.ROOT, "%.1f MB", (double) bytes / MB);
        }
        if (bytes < TB) {
            return String.format(Locale.ROOT, "%.2f GB", (double) bytes / GB);
        }
        return String.format(Locale.ROOT, "%.2f TB", (double) bytes / TB);
    }
}
