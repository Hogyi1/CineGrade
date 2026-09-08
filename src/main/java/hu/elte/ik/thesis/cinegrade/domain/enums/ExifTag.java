package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum ExifTag {
    DATE_TIME_ORIGINAL("DateTimeOriginal"),
    MAKE("Make"),
    MODEL("Model"),
    LENS_MODEL("LensModel"),
    FOCAL_LENGTH("FocalLength"),
    F_NUMBER("FNumber"),
    EXPOSURE_TIME("ExposureTime"),
    ISO("ISO"),
    FILE_TYPE("FileType"),
    FILE_SIZE("FileSize"),
    COLOR_SPACE("ColorSpace"),
    COLOR_MODE("-PictureStyle -PictureControlName -FilmMode -CreativeStyle -PhotoStyle -PictureMode");

    private final String tagName;

    ExifTag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }
}
