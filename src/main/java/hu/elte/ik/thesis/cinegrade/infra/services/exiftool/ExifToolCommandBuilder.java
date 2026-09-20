package hu.elte.ik.thesis.cinegrade.infra.services.exiftool;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.process.NativeBinaryLocator;
import hu.elte.ik.thesis.cinegrade.infra.services.CommandBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExifToolCommandBuilder implements CommandBuilder {

    private final List<String> args = new ArrayList<>();
    private final List<String> files = new ArrayList<>();
    private final String EXIFTOOL_EXECUTABLE;

    public ExifToolCommandBuilder() {
        EXIFTOOL_EXECUTABLE = NativeBinaryLocator.getExecutablePath("exiftool")
                .map(Path::toString)
                .orElseThrow(() -> new CineGradeException(ErrorCode.REQ_EXIFTOOL_NOT_FOUND));
    }

    /**
     * Outputs all metadata as a JSON array (parseable by Gson).
     */
    public ExifToolCommandBuilder asJson() {
        args.add("-json");
        return this;
    }

    /**
     * Enforces UTF-8 character encoding for metadata with special characters.
     */
    public ExifToolCommandBuilder withUtf8() {
        args.addAll(List.of("-charset", "UTF8"));
        return this;
    }

    /**
     * Skips scanning file trailers (crucial speedup for large RAW files).
     */
    public ExifToolCommandBuilder fastMode() {
        args.add("-fast");
        return this;
    }

    /**
     * Overwrites the target file in-place without creating a 'filename_original' backup.
     */
    public ExifToolCommandBuilder overwriteOriginal() {
        args.add("-overwrite_original");
        return this;
    }

    /**
     * Requests specific EXIF/XMP tags (e.g. "ISO", "Model", "FNumber").
     */
    public ExifToolCommandBuilder addTag(ExifTag tag) {
        if (tag != null) {
            args.add(tag.getTagName());
        }
        return this;
    }

    public ExifToolCommandBuilder addTags(List<ExifTag> tags) {
        if (tags != null) {
            tags.forEach(this::addTag);
        }
        return this;
    }

    public ExifToolCommandBuilder addTags(ExifTag... tags) {
        for (ExifTag tag : tags) {
            addTag(tag);
        }
        return this;
    }

    public ExifToolCommandBuilder versionInfo() {
        args.add("-ver");
        return this;
    }

    /**
     * Sets or updates a tag value (e.g. tag="Rating", value=5 -> "-Rating=5").
     */
    public ExifToolCommandBuilder setTagValue(String tag, Object value) {
        if (tag != null && value != null) {
            args.add(String.format("-%s=%s", tag.trim().replace("-", ""), value));
        }
        return this;
    }

    /**
     * Helper specifically for setting 1-5 star ratings.
     */
    public ExifToolCommandBuilder setRating(int rating) {
        return setTagValue("Rating", Math.clamp(rating, 0, 5));
    }

    /**
     * Target files to inspect or modify (supports single or bulk files).
     */
    public ExifToolCommandBuilder addTarget(Path file) {
        if (file != null) {
            files.add(file.toAbsolutePath().toString());
        }
        return this;
    }

    public ExifToolCommandBuilder addTargets(List<Path> targetFiles) {
        if (targetFiles != null) {
            targetFiles.forEach(this::addTarget);
        }
        return this;
    }

    public ArrayList<String> buildCommands() {
        ArrayList<String> finalCommands = new ArrayList<>();
        finalCommands.add(EXIFTOOL_EXECUTABLE);
        finalCommands.addAll(args);
        finalCommands.addAll(files);
        return finalCommands;
    }
}

