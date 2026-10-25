// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EditState {
    private String photoId;
    private double exposure = 0.0;
    private double contrast = 0.0;
    private double highlights = 0.0;
    private double shadows = 0.0;
    private double whites = 0.0;
    private double blacks = 0.0;
    private double temperature = 5500.0;
    private double tint = 0.0;
    private double saturation = 0.0;
    private double vibrance = 0.0;
    private String lutPath;
    private String curvesJson;
    private Instant updatedAt = Instant.now();

    // [AI-GENERATED METHOD]
    public EditState() {
    }

    // [AI-GENERATED METHOD]
    public EditState(String photoId) {
        this.photoId = photoId;
    }

    // [AI-GENERATED METHOD]
    public EditState(String photoId, double exposure, double contrast, double highlights, double shadows, double whites, double blacks, double temperature, double tint, double saturation, double vibrance, String lutPath, String curvesJson, Instant updatedAt) {
        this.photoId = photoId;
        this.exposure = exposure;
        this.contrast = contrast;
        this.highlights = highlights;
        this.shadows = shadows;
        this.whites = whites;
        this.blacks = blacks;
        this.temperature = temperature;
        this.tint = tint;
        this.saturation = saturation;
        this.vibrance = vibrance;
        this.lutPath = lutPath;
        this.curvesJson = curvesJson;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }
}
