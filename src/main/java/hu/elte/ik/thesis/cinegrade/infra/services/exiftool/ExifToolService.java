package hu.elte.ik.thesis.cinegrade.infra.services.exiftool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag;
import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;

public class ExifToolService {

    private static final Logger logger = LogManager.getLogger(ExifToolService.class);
    private final int TIMEOUT_MILLIS = 5000;

    public PhotoMetadata readMetaData(Path path, BooleanSupplier isCanceled) {
        logger.debug("Reading metadata for: {}", path);
        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();
        String colorProfile = "";

        ArrayList<String> commands = builder.addTags(MAKE, MODEL, F_NUMBER, EXPOSURE_TIME, ISO, FOCAL_LENGTH, DATE_TIME_ORIGINAL, COLOR_MODE)
                .asJson()
                .withUtf8()
                .fastMode()
                .addTarget(path)
                .buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        if (!pr.isSuccess()) {
            logger.debug("ExifTool process failed for '{}': exitCode={}, error={}", path, pr.exitCode(), pr.stderr());
        }

        JsonObject jsonObject = new Gson().fromJson(pr.stdout(), JsonObject.class);

        for (var entry : jsonObject.asMap().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            boolean hasColorProfile = COLOR_MODE.getTagName().toLowerCase().contains(key.toLowerCase());
            if (hasColorProfile && !(value == null || value.isJsonNull())) {
                colorProfile = value.getAsString();
            }
        }

        PhotoMetadata photoMetadata = new Gson().fromJson(pr.stdout(), PhotoMetadata.class);
        photoMetadata.setColorStyle(colorProfile);
        logger.debug("Successfully read metadata for '{}', color profile: '{}'", path, colorProfile);
        return photoMetadata;
    }

    public Optional<String> checkExifToolVersion(BooleanSupplier isCanceled) {
        logger.debug("Checking ExifTool version...");
        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();

        ArrayList<String> commands = builder.versionInfo().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        if (pr.isSuccess()) {
            String version = pr.stdout().trim();
            logger.debug("ExifTool version detected: {}", version);
            return Optional.of(version);
        }
        logger.debug("ExifTool version check failed: exitCode={}, error={}", pr.exitCode(), pr.stderr());
        return Optional.empty();
    }

    public boolean isAvailable() {
        boolean available = checkExifToolVersion(null).isPresent();
        logger.debug("ExifTool availability: {}", available);
        return available;
    }

    public boolean isImage(Path path, BooleanSupplier isCanceled) {
        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();
        builder.addTags(MIME_TYPE).addTarget(path);
        ArrayList<String> commands = builder.buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        return pr.stdout().contains("image/");
    }

    public List<FileProbeResult> probeFiles(List<Path> pathList, BooleanSupplier isCanceled) {
        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();
        List<String> commands = builder.asJson()
                .addTags(FILE_SIZE, FILE_TYPE, IMAGE_SIZE, MIME_TYPE)
                .addTargets(pathList)
                .buildCommands();

        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        // Process the results and return a list of FileProbeResult objects
        if (!pr.isSuccess()) {
            logger.error("Failed to probe batch files exitCode={}, error={}", pr.exitCode(), pr.stderr());
            return List.of();
        }

        Type listType = new TypeToken<ArrayList<FileProbeResult>>(){}.getType();
        return new Gson().fromJson(pr.stdout(), listType);
    }
}
