package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

public class StageEntry {
    private File file;
    private Path thumbnailPath;
    private Path jpegPath;
    private final BooleanProperty isSelectedProperty = new SimpleBooleanProperty(true);
    private boolean isRaw;
    private long size;
    private String fileType;

    public StageEntry() {
    }

    public StageEntry(File file, Path thumbnailPath, Path jpegPath, boolean isRaw, long size) {
        this(file, thumbnailPath, jpegPath, isRaw, size, null);
    }

    public StageEntry(File file, Path thumbnailPath, Path jpegPath, boolean isRaw, long size, String fileType) {
        this.file = file;
        this.thumbnailPath = thumbnailPath;
        this.jpegPath = jpegPath;
        this.isRaw = isRaw;
        this.size = size;
        this.fileType = fileType;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Path getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(Path thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Path getJpegPath() {
        return jpegPath;
    }

    public void setJpegPath(Path jpegPath) {
        this.jpegPath = jpegPath;
    }

    public boolean isSelected() {
        return isSelectedProperty.get();
    }

    public void setSelected(boolean selected) {
        this.isSelectedProperty.set(selected);
    }

    public BooleanProperty isSelectedProperty() {
        return isSelectedProperty;
    }

    public boolean isRaw() {
        return isRaw;
    }

    public void setRaw(boolean raw) {
        isRaw = raw;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
