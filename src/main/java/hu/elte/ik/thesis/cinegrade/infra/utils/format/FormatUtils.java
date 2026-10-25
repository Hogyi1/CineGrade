// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.utils.format;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Utility class providing static formatting helpers for data sizes, timestamps, and strings.
 */
public final class FormatUtils {

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final long TB = GB * 1024L;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

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

    /**
     * Formats an instant as a calendar date in the system time zone (e.g. 2026-10-25).
     *
     * @param instant the instant to format, may be null
     * @return formatted date, or an empty string if the instant is null
     */
    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Formats an instant relative to now:
     * - Just now
     * - 5 minutes ago
     * - 1 hour ago
     * - Yesterday
     * - 3 months ago
     * - 2 years ago
     *
     * @param instant the past instant to format, may be null
     * @return relative time string, or "Never" if the instant is null
     */
    public static String formatRelativeTime(Instant instant) {
        if (instant == null) {
            return "Never";
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime then = instant.atZone(zone);
        ZonedDateTime now = Instant.now().atZone(zone);

        long years = ChronoUnit.YEARS.between(then, now);
        if (years > 0) return years == 1 ? "1 year ago" : years + " years ago";

        long months = ChronoUnit.MONTHS.between(then, now);
        if (months > 0) return months == 1 ? "1 month ago" : months + " months ago";

        long days = ChronoUnit.DAYS.between(then, now);
        if (days > 0) return days == 1 ? "Yesterday" : days + " days ago";

        long hours = ChronoUnit.HOURS.between(then, now);
        if (hours > 0) return hours == 1 ? "1 hour ago" : hours + " hours ago";

        long minutes = ChronoUnit.MINUTES.between(then, now);
        if (minutes > 0) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";

        return "Just now";
    }
}
