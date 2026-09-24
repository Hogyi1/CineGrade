package hu.elte.ik.thesis.cinegrade.domain.catalog;

import org.sqlite.date.DateFormatUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class Catalog {

    private int id;
    private String catalogName;
    private String catalogPath;
    private Instant lastOpenedAt;
    private Instant createdAt;
    private transient long size = 0L;

    public Catalog(int id, String catalogName, String catalogPath, Instant lastOpenedAt, Instant createdAt) {
        this.id = id;
        this.catalogName = catalogName;
        this.catalogPath = catalogPath;
        this.lastOpenedAt = lastOpenedAt;
        this.createdAt = createdAt;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormattedSize() {
        if (size <= 0) return "0 MB";
        double mb = size / (1024.0 * 1024.0);
        return String.format("%.1f MB", mb);
    }

    public String getCatalogName() {
        return catalogName;
    }

    public String getCatalogPath() {
        return catalogPath;
    }

    public int getId() {
        return id;
    }

    public Instant getLastOpenedAt() {
        return lastOpenedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(Date.from(createdAt));
    }

    public String getFormattedLastOpenedAt() {

        Instant now = Instant.now();

        long years = ChronoUnit.YEARS.between(lastOpenedAt, now);
        if (years > 0) return years == 1 ? "1 year ago" : years + " years ago";

        long months = ChronoUnit.MONTHS.between(lastOpenedAt, now);
        if (months > 0) return months == 1 ? "1 month ago" : months + " months ago";

        long days = ChronoUnit.DAYS.between(lastOpenedAt, now);
        if (days > 0) return days == 1 ? "Yesterday" : days + " days ago";

        long hours = ChronoUnit.HOURS.between(lastOpenedAt, now);
        if (hours > 0) return hours == 1 ? "1 hour ago" : hours + " hours ago";

        long minutes = ChronoUnit.MINUTES.between(lastOpenedAt, now);
        if (minutes > 0) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";

        return "Just now";
    }
}
