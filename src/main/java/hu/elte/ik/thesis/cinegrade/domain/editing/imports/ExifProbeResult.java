package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import com.google.gson.annotations.SerializedName;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;

import java.util.Arrays;

public record ExifProbeResult(
        @SerializedName("SourceFile") String sourceFile,
        @SerializedName("DateTimeOriginal") String dateTimeOriginal,
        @SerializedName("Make") String make,
        @SerializedName("Model") String model,
        @SerializedName("LensModel") String lensModel,
        @SerializedName("LensID") String lensId,
        @SerializedName("FocalLength") String focalLength,
        @SerializedName("FNumber") String fNumber,
        @SerializedName("ExposureTime") String exposureTime,
        @SerializedName("ISO") String iso,
        @SerializedName("ColorSpace") String colorSpace,
        @SerializedName("PictureStyle") String pictureStyle,
        @SerializedName("PictureControlName") String pictureControlName,
        @SerializedName("FilmMode") String filmMode,
        @SerializedName("CreativeStyle") String creativeStyle,
        @SerializedName("PhotoStyle") String photoStyle,
        @SerializedName("PictureMode") String pictureMode
) {

    /** First non-blank manufacturer style tag, or "Standard" if none exists. */
    public String colorStyle() {
        return Arrays.stream(new String[]{
                        pictureStyle, pictureControlName, filmMode,
                        creativeStyle, photoStyle, pictureMode})
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse("Standard");
    }

    public String lens() {
        if (lensModel != null && !lensModel.isBlank()) return lensModel;
        return lensId;
    }

    public PhotoMetadata toPhotoMetadata(String photoId, String rawMetadataJson) {
        return new PhotoMetadata(
                photoId,
                make,
                model,
                lens(),
                parseInt(iso),
                parseDouble(fNumber),
                exposureTime,
                focalLength,
                dateTimeOriginal,
                colorStyle(),
                rawMetadataJson
        );
    }

    private static int parseInt(String s) {
        try {
            return s == null ? 0 : (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return s == null ? 0.0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}