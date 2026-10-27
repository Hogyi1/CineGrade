// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public class PhotoEntry extends Entry {

    private final BooleanProperty missingPhoto = new SimpleBooleanProperty(false);
    private final BooleanProperty missingThumbnail = new SimpleBooleanProperty(false);
    @Getter @Setter
    private String id;
    // Setters below are overloaded with Path variants, so only the getter is generated
    @Getter
    private String originalPath;
    @Getter @Setter
    private ImportMethod importMethod;
    @Getter @Setter
    private long fileSize;
    @Getter @Setter
    private int rating = 0;
    @Getter @Setter
    private String colorLabel;
    @Getter @Setter
    private Instant dateImported;
    @Getter @Setter
    private String importId;
    @Getter @Setter
    private String mimeType;
    @Getter @Setter
    private boolean isRaw;
    @Getter @Setter
    private int rotation;

    // On import
    public PhotoEntry(File file) {
        super(file);
        this.id = UUID.randomUUID().toString();
        this.fileSize = file != null ? file.length() : 0L;
        this.dateImported = Instant.now();
    }

    // On DAO
    public PhotoEntry(String id, String importId, File resolvedFile,
                      long fileSize, int rating, String colorLabel,
                      Instant dateImported, ImportMethod importMethod, String originalPath,
                      String mimeType, boolean isRaw, int rotation) {
        super(resolvedFile);
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.importId = importId;
        this.fileSize = fileSize;
        this.rating = rating;
        this.colorLabel = colorLabel;
        this.dateImported = dateImported != null ? dateImported : Instant.now();
        this.importMethod = importMethod != null ? importMethod : ImportMethod.ADD;
        this.originalPath = originalPath;
        this.mimeType = mimeType;
        this.isRaw = isRaw;
        this.rotation = rotation;

        if (!Files.exists(file.toPath())) {
            this.missingPhoto.set(true);
        }
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

    public void setOriginalPath(Path originalPath) {
        this.originalPath = originalPath.toString();
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
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
