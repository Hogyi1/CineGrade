// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.domain.editing;

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

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getLens() {
        return lens;
    }

    public void setLens(String lens) {
        this.lens = lens;
    }

    public int getIso() {
        return iso;
    }

    public void setIso(int iso) {
        this.iso = iso;
    }

    public double getAperture() {
        return aperture;
    }

    public void setAperture(double aperture) {
        this.aperture = aperture;
    }

    public String getShutterSpeed() {
        return shutterSpeed;
    }

    public void setShutterSpeed(String shutterSpeed) {
        this.shutterSpeed = shutterSpeed;
    }

    public String getFocalLength() {
        return focalLength;
    }

    public void setFocalLength(String focalLength) {
        this.focalLength = focalLength;
    }

    public String getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(String captureDate) {
        this.captureDate = captureDate;
    }

    public String getColorStyle() {
        return colorStyle;
    }

    public void setColorStyle(String colorStyle) {
        this.colorStyle = (colorStyle == null || colorStyle.isBlank()) ? "Standard" : colorStyle;
    }

    public String getRawMetadataJson() {
        return rawMetadataJson;
    }

    public void setRawMetadataJson(String rawMetadataJson) {
        this.rawMetadataJson = rawMetadataJson;
    }
}
