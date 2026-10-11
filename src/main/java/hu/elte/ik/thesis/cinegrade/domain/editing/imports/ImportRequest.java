package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import java.io.File;
import java.nio.file.Path;

public record ImportRequest(File file, Path jpegPath, Path thumbnailPath, boolean isRaw) {
    public static ImportRequest from(StageEntry stageEntry) {
        return new ImportRequest(stageEntry.getFile(), stageEntry.jpegPath, stageEntry.thumbnailPath, stageEntry.isRaw);
    }
}

