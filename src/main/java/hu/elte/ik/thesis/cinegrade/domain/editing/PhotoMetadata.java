// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhotoMetadata {
    private String photoId;
    private String make;
    private String model;
    private String lens;
    private int iso;
    private double aperture;
    private String shutterSpeed;
    private String focalLength;
    private String captureDate;
    private String colorStyle = "Standard";
    private String rawMetadataJson;

    // [AI-GENERATED METHOD]
    public PhotoMetadata() {
    }

    // [AI-GENERATED METHOD]
    public PhotoMetadata(String photoId, String make, String model, String lens, int iso, double aperture, String shutterSpeed, String focalLength, String captureDate, String colorStyle, String rawMetadataJson) {
        this.photoId = photoId;
        this.make = make;
        this.model = model;
        this.lens = lens;
        this.iso = iso;
        this.aperture = aperture;
        this.shutterSpeed = shutterSpeed;
        this.focalLength = focalLength;
        this.captureDate = captureDate;
        this.colorStyle = (colorStyle == null || colorStyle.isBlank()) ? "Standard" : colorStyle;
        this.rawMetadataJson = rawMetadataJson;
    }

    public void setColorStyle(String colorStyle) {
        this.colorStyle = (colorStyle == null || colorStyle.isBlank()) ? "Standard" : colorStyle;
    }
}
