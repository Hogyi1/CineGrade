package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import hu.elte.ik.thesis.cinegrade.domain.editing.Entry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
public class StageEntry extends Entry {

    private final BooleanProperty isSelectedProperty = new SimpleBooleanProperty(true);
    // Import panel duplication
    private final BooleanProperty isMaybeDuplicateProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty isCompanionProperty = new SimpleBooleanProperty(false);

    @Setter
    private transient Image decodedThumbnail;

    // File specific
    public byte[] thumbnail;
    // Card specific
    public boolean isRaw;
    public String mimeType;
    public long size;
    public String fileType;
    public String fileSize;
    public String imageSize;
    public int rotation;

    // Staging deduplication
    public Path jpegPath;
    public StageEntry companion;

    public StageEntry(FileProbeResult result) {
        super(result.toFile());
        this.isRaw = result.isRaw();
        this.mimeType = result.mimeType();
        this.fileType = result.fileType();
        this.imageSize = result.imageSize();
        this.size = result.toFile().length();
        this.fileSize = result.fileSize();
        this.rotation = result.getRotation();
    }
}
