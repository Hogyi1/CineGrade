package hu.elte.ik.thesis.cinegrade.domain.catalog;

public record PhotoMetadata(
        String make,
        String model,
        String lens,
        int iso,
        double aperture,
        String shutterSpeed,
        String colorStyle,
        String captureDate) {

}
