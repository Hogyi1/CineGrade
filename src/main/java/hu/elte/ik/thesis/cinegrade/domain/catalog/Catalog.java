package hu.elte.ik.thesis.cinegrade.domain.catalog;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

@Getter
public class Catalog {

    private final int id;
    private final String catalogName;
    // Custom getters below return these as Paths
    private final String projectDirectory;
    private final String projectFilePath;
    private final Instant createdAt;
    @Setter
    private Instant lastOpenedAt;
    @Setter
    private transient long size = 0L;

    public Catalog(int id, String catalogName, String projectDirectory, String projectFilePath, Instant lastOpenedAt, Instant createdAt) {
        this.id = id;
        this.catalogName = catalogName;
        this.projectDirectory = projectDirectory;
        this.projectFilePath = projectFilePath;
        this.lastOpenedAt = lastOpenedAt;
        this.createdAt = createdAt;
    }

    /** The folder that holds the .cgproj file and everything that belongs to it. */
    public Path getProjectDirectory() {
        return Path.of(projectDirectory);
    }

    /** The .cgproj SQLite file. */
    public Path getProjectFilePath() {
        return Path.of(projectFilePath);
    }

    public Path getTempDirectory() {
        return getProjectDirectory().resolve("temp");
    }

    public Path getCacheDirectory() {
        return getProjectDirectory().resolve("cache");
    }

    public Path getTempImportsDirectory() {
        return getTempDirectory().resolve("imports");
    }

    public Path getMediaDirectory() {
        return getProjectDirectory().resolve("media");
    }

    public Path getMediaImportDirectory() {
        return getMediaDirectory().resolve("imports");
    }

    /** Thumbnail/preview BLOB cache, safe to delete. */
    public Path getPreviewDatabasePath() {
        return getCacheDirectory().resolve("previews.cgpreviews");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Catalog catalog = (Catalog) o;
        return id == catalog.id && Objects.equals(catalogName, catalog.catalogName) && Objects.equals(projectFilePath, catalog.projectFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, catalogName, projectFilePath);
    }
}
