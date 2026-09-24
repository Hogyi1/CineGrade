package hu.elte.ik.thesis.cinegrade.domain.editing;

public class PhotoMetadata{
        String make;
        String model;
        String lens;
        int iso;
        double aperture;
        String shutterSpeed;
        String colorStyle = "Standard";
        String captureDate;

    public void setColorStyle(String colorStyle) {
        this.colorStyle = colorStyle.isBlank() ? this.colorStyle = "Standard" : colorStyle;
    }
}
