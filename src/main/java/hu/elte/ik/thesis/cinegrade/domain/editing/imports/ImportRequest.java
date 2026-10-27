package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import java.io.File;
import java.nio.file.Path;

/**
 * What the import needs from a staged entry. mimeType, isRaw and rotation are stored on the photo row,
 * so a missing thumbnail can be regenerated later with the right decoder and orientation.
 */
public record ImportRequest(File file, Path jpegPath, byte[] thumbnail, boolean isRaw, String mimeType, int rotation) {
    public static ImportRequest from(StageEntry stageEntry) {
        return new ImportRequest(stageEntry.getFile(), stageEntry.jpegPath, stageEntry.thumbnail,
                stageEntry.isRaw, stageEntry.mimeType, stageEntry.rotation);
    }
}
