package hu.elte.ik.thesis.cinegrade.domain.catalog;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

public class Catalog {

    private final int id;
    private final String catalogName;
    private final String catalogDirectoryPath;
    private final Instant lastOpenedAt;
    private final Instant createdAt;
    private transient long size = 0L;

    public Catalog(int id, String catalogName, String catalogDirectoryPath, Instant lastOpenedAt, Instant createdAt) {
        this.id = id;
        this.catalogName = catalogName;
        this.catalogDirectoryPath = catalogDirectoryPath;
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

    public Path getCatalogDirectoryPath() {
        return Path.of(catalogDirectoryPath);
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

        if (lastOpenedAt == null) {
            return "Never";
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime opened = lastOpenedAt.atZone(zone);
        ZonedDateTime now = Instant.now().atZone(zone);

        long years = ChronoUnit.YEARS.between(opened, now);
        if (years > 0) return years == 1 ? "1 year ago" : years + " years ago";

        long months = ChronoUnit.MONTHS.between(opened, now);
        if (months > 0) return months == 1 ? "1 month ago" : months + " months ago";

        long days = ChronoUnit.DAYS.between(opened, now);
        if (days > 0) return days == 1 ? "Yesterday" : days + " days ago";

        long hours = ChronoUnit.HOURS.between(opened, now);
        if (hours > 0) return hours == 1 ? "1 hour ago" : hours + " hours ago";

        long minutes = ChronoUnit.MINUTES.between(opened, now);
        if (minutes > 0) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";

        return "Just now";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Catalog catalog = (Catalog) o;
        return id == catalog.id && Objects.equals(catalogName, catalog.catalogName) && Objects.equals(catalogDirectoryPath, catalog.catalogDirectoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, catalogName, catalogDirectoryPath);
    }

    public Path getTempDirectory() {
        return getCatalogDirectoryPath().resolve("temp");
    }

    public Path getSettingsDirectory() {
        return getCatalogDirectoryPath().resolve("settings");
    }

    public Path getTempImportsDirectory() { return getTempDirectory().resolve("imports"); }

    public Path getMediaDirectory() {
        return getCatalogDirectoryPath().resolve("media");
    }

    public Path getThumbnailsDirectory() {
        return getCatalogDirectoryPath().resolve("cache").resolve("thumbnails");
    }
}
