// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

import java.time.Instant;

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
    public EditState(String photoId, double exposure, double contrast, double highlights, double shadows, double whites, double blacks, double temperature, double tint, double saturation, double vibrance, String lutPath, String curvesJson, java.time.Instant updatedAt) {
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
        this.updatedAt = updatedAt != null ? updatedAt : java.time.Instant.now();
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public double getExposure() {
        return exposure;
    }

    public void setExposure(double exposure) {
        this.exposure = exposure;
    }

    public double getContrast() {
        return contrast;
    }

    public void setContrast(double contrast) {
        this.contrast = contrast;
    }

    public double getHighlights() {
        return highlights;
    }

    public void setHighlights(double highlights) {
        this.highlights = highlights;
    }

    public double getShadows() {
        return shadows;
    }

    public void setShadows(double shadows) {
        this.shadows = shadows;
    }

    public double getWhites() {
        return whites;
    }

    public void setWhites(double whites) {
        this.whites = whites;
    }

    public double getBlacks() {
        return blacks;
    }

    public void setBlacks(double blacks) {
        this.blacks = blacks;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTint() {
        return tint;
    }

    public void setTint(double tint) {
        this.tint = tint;
    }

    public double getSaturation() {
        return saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getVibrance() {
        return vibrance;
    }

    public void setVibrance(double vibrance) {
        this.vibrance = vibrance;
    }

    public String getLutPath() {
        return lutPath;
    }

    public void setLutPath(String lutPath) {
        this.lutPath = lutPath;
    }

    public String getCurvesJson() {
        return curvesJson;
    }

    public void setCurvesJson(String curvesJson) {
        this.curvesJson = curvesJson;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
