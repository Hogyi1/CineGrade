package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum ExifTag {
    DATE_TIME_ORIGINAL("DateTimeOriginal"),
    MAKE("Make"),
    MODEL("Model"),
    LENS_MODEL("LensModel"),
    LENS_ID("LensID"),              // fallback when LensModel is missing
    FOCAL_LENGTH("FocalLength"),
    F_NUMBER("FNumber"),
    EXPOSURE_TIME("ExposureTime"),
    ISO("ISO"),
    FILE_TYPE("FileType"),
    IMAGE_SIZE("ImageSize"),
    MIME_TYPE("MIMEType"),
    ORIENTATION("Orientation"),
    FILE_SIZE("FileSize"),
    COLOR_SPACE("ColorSpace"),

    // Color style / picture profile, one tag per manufacturer
    PICTURE_STYLE("PictureStyle"),              // Canon
    PICTURE_CONTROL_NAME("PictureControlName"), // Nikon
    FILM_MODE("FilmMode"),                      // Fujifilm
    CREATIVE_STYLE("CreativeStyle"),            // Sony
    PHOTO_STYLE("PhotoStyle"),                  // Panasonic
    PICTURE_MODE("PictureMode");                // Olympus / Pentax

    private final String tagName;

    ExifTag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }
}