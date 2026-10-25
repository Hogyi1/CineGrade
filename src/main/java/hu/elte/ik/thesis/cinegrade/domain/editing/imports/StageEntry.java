package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import hu.elte.ik.thesis.cinegrade.domain.editing.Entry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

import java.nio.file.Path;

public class StageEntry extends Entry {

    @Getter
    private final BooleanProperty isSelectedProperty = new SimpleBooleanProperty(true);
    // Import panel duplication
    @Getter
    private final BooleanProperty isMaybeDuplicateProperty = new SimpleBooleanProperty(false);
    @Getter
    private final BooleanProperty isCompanionProperty = new SimpleBooleanProperty(false);

    // File specific
    public Path thumbnailPath;
    // Card specific
    public boolean isRaw;
    public long size;
    public String fileType;
    public String fileSize;
    public String imageSize;
    public int rotation;

    // Staging deduplication
    public Path jpegPath;
    public boolean isCompanion;
    public StageEntry companion;

    public StageEntry(FileProbeResult result) {
        super(result.toFile());
        this.isRaw = result.isRaw();
        this.fileType = result.fileType();
        this.imageSize = result.imageSize();
        this.size = result.toFile().length();
        this.fileSize = result.fileSize();
        this.rotation = result.getRotation();
    }

    public void toggleSelected() {
        isSelectedProperty.set(!isSelectedProperty.get());
    }
}
