// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public class PhotoEntry extends Entry {

    private final BooleanProperty missingPhoto = new SimpleBooleanProperty(false);
    private final BooleanProperty missingThumbnail = new SimpleBooleanProperty(false);
    private String id;
    private String thumbnailPath;
    private String originalPath;
    private ImportMethod importMethod;
    private long fileSize;
    private int rating = 0;
    private String colorLabel;
    private Instant dateImported;
    private String importId;

    // On import
    public PhotoEntry(File file) {
        super(file);
        this.id = UUID.randomUUID().toString();
        this.fileSize = file != null ? file.length() : 0L;
        this.dateImported = Instant.now();

        // setFile(getResolvedFile(catalogPath));
    }

    // On DAO
    public PhotoEntry(String id, String importId, File resolvedFile, String thumbnailPath,
                      long fileSize, int rating, String colorLabel,
                      Instant dateImported, ImportMethod importMethod, String originalPath) {
        super(resolvedFile);
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.importId = importId;
        this.thumbnailPath = thumbnailPath;
        this.fileSize = fileSize;
        this.rating = rating;
        this.colorLabel = colorLabel;
        this.dateImported = dateImported != null ? dateImported : Instant.now();
        this.importMethod = importMethod != null ? importMethod : ImportMethod.ADD;
        this.originalPath = originalPath;

        if (thumbnailPath == null || thumbnailPath.isEmpty()) {
            this.missingThumbnail.set(true);
        }
        if (!Files.exists(file.toPath())) {
            this.missingPhoto.set(true);
        }
    }

    /**
     * Resolves the thumbnail file to an absolute File on disk.
     */
    public File getResolvedThumbnailFile(Path catalogPath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return null;
        }
        Path path = Path.of(thumbnailPath);
        if (path.isAbsolute() || catalogPath == null) {
            return path.toFile();
        }
        return catalogPath.resolve(path).toFile();
    }

    /**
     * Converts the photo's path into what should be stored in the database.
     * If COPY/MOVE and inside the catalog, returns relative path (e.g. "media/Import_xxx/pic.jpg").
     * If ADD, returns absolute path.
     */
    public String getDatabaseFilePath(Path catalogPath) {
        if (file == null) {
            return "";
        }
        if (importMethod == ImportMethod.ADD || catalogPath == null) {
            // Stored in place outside the project -> keep absolute
            return file.getAbsolutePath().replace('\\', '/');
        }

        Path filePath = file.toPath().toAbsolutePath().normalize();
        Path normCatalog = catalogPath.toAbsolutePath().normalize();

        if (filePath.startsWith(normCatalog)) {
            return normCatalog.relativize(filePath).toString().replace('\\', '/');
        }

        return file.getAbsolutePath().replace('\\', '/');
    }

    /**
     * Converts thumbnail path to relative path for database storage.
     */
    public String getDatabaseThumbnailPath(Path catalogPath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return null;
        }

        Path tPath = Path.of(thumbnailPath);
        if (!tPath.isAbsolute()) {
            // Already a relative path! Just normalize slashes
            return thumbnailPath.replace('\\', '/');
        }

        if (catalogPath != null) {
            Path normCatalog = catalogPath.toAbsolutePath().normalize();
            Path normThumb = tPath.toAbsolutePath().normalize();

            if (normThumb.startsWith(normCatalog)) {
                // Option A: "cache/thumbnails/..."
                return normCatalog.relativize(normThumb).toString().replace('\\', '/');

                // Or Option B (with project name):
                // return normCatalog.getParent().relativize(normThumb).toString().replace('\\', '/');
            }
        }

        return thumbnailPath.replace('\\', '/');
    }

    // Getters and Setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(Path thumbnailPath) {
        this.thumbnailPath = thumbnailPath.toString();
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(Path originalPath) {
        this.originalPath = originalPath.toString();
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public ImportMethod getImportMethod() {
        return importMethod;
    }

    public void setImportMethod(ImportMethod importMethod) {
        this.importMethod = importMethod;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getColorLabel() {
        return colorLabel;
    }

    public void setColorLabel(String colorLabel) {
        this.colorLabel = colorLabel;
    }

    public Instant getDateImported() {
        return dateImported;
    }

    public void setDateImported(Instant dateImported) {
        this.dateImported = dateImported;
    }

    public String getImportId() {
        return importId;
    }

    public void setImportId(String importId) {
        this.importId = importId;
    }

    public BooleanProperty missingPhotoProperty() {
        return missingPhoto;
    }

    public boolean isMissingPhoto() {
        return missingPhoto.get();
    }

    public void setMissingPhoto(boolean missing) {
        this.missingPhoto.set(missing);
    }

    public BooleanProperty missingThumbnailProperty() {
        return missingThumbnail;
    }

    public boolean isMissingThumbnail() {
        return missingThumbnail.get();
    }

    public void setMissingThumbnail(boolean missing) {
        this.missingThumbnail.set(missing);
    }

    public String getFilePath() {
        return file != null ? file.getAbsolutePath() : "";
    }

    public String getFileName() {
        return file != null ? file.getName() : "";
    }
}
