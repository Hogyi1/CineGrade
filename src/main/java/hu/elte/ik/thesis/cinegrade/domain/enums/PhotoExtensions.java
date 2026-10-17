package hu.elte.ik.thesis.cinegrade.domain.enums;

import java.util.List;

public enum PhotoExtensions {
    JPG("JPG"),
    JPEG("JPEG"),
    PNG("PNG"),
    PANASONIC_RAW("RW2"),
    PANASONIC_RAW_RAW("RAW"),
    CANON_RAW_CR3("CR3"),
    CANON_RAW_CR2("CR2"),
    CANON_RAW_CRW("CRW"),
    SONY_RAW("ARW"),
    SONY_RAW_SR2("SR2"),
    SONY_RAW_SRF("SRF"),
    FUJIFILM_RAW("RAF"),
    NIKON_RAW_NEF("NEF"),
    NIKON_RAW_NRW("NRW"),
    NIKON_RAW_NEFX("NEFX"),
    OLYMPUS_RAW("ORW"),
    OLYMPUS_RAW_ORF("ORF"),
    OLYMPUS_RAW_ORI("ORI"),
    ADOBE_RAW_DNG("DNG"),
    PENTAX_RAW_PEF("PEF"),
    LEICA_RAW_RWL("RWL"),
    HASSELBLAD_RAW_3FR("3FR"),
    HASSELBLAD_RAW_FFF("FFF"),
    TIFF("TIF"),
    TIFF_EXT("TIFF");

    private final String extension;

    PhotoExtensions(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
    public boolean isRaw() {
        return switch (this) {
            case JPG, JPEG, PNG, TIFF, TIFF_EXT -> false;
            default -> true;
        };
    }

    public static List<String> getAllWildcardPatterns() {
        List<String> patterns = new java.util.ArrayList<>();
        for (PhotoExtensions ext : values()) {
            String lower = "*." + ext.getExtension().toLowerCase();
            if (!patterns.contains(lower)) {
                patterns.add(lower);
            }
        }
        for (PhotoExtensions ext : values()) {
            String upper = "*." + ext.getExtension().toUpperCase();
            if (!patterns.contains(upper)) {
                patterns.add(upper);
            }
        }
        return patterns;
    }
}
