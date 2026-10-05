package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.Set;

public record FileProbeResult(
        @SerializedName("SourceFile") String sourceFile,
        @SerializedName("MIMEType") String mimeType,
        @SerializedName("FileType") String fileType,
        @SerializedName("FileSize") String fileSize,
        @SerializedName("ImageSize") String imageSize
) {
    private static final Set<String> RAW_TYPES = Set.of(
            "CR2", "CR3", "NEF", "ARW", "DNG", "RAF", "RW2", "ORF", "PEF"
    );

    public boolean isImage() {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }

    public boolean isRaw() {
        return fileType != null && RAW_TYPES.contains(fileType.toUpperCase());
    }

    public Path toPath() {
        return Path.of(sourceFile);
    }
}