package hu.elte.ik.thesis.cinegrade.infra.services.exiftool;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.ExifProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag;
import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;

import static hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;

public class ExifToolService {

    private static final Logger logger = LogManager.getLogger(ExifToolService.class);
    private static final int CHUNK_SIZE = 50;
    private static final Gson GSON = new Gson();
    private final int TIMEOUT_MILLIS = 60000;

    /**
     * Reads metadata from a file.
     *
     * @param path       the file path to read metadata from
     * @param photoId    the ID of the photo
     * @param isCanceled a supplier to check if the operation is canceled
     * @return an Optional containing the photo metadata if successful, empty otherwise
     */
    public Optional<PhotoMetadata> readMetaData(Path path, String photoId, BooleanSupplier isCanceled) {
        logger.debug("Reading metadata for: {}", path);

        List<String> commands = new ExifToolCommandBuilder()
                .addTags(ExifTag.values())
                .asJson()
                .withUtf8()
                .fastMode()
                .addTarget(path)
                .buildCommands();

        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        if (!pr.isSuccess() || pr.stdout() == null || pr.stdout().isBlank()) {
            logger.debug("ExifTool failed for '{}': exitCode={}, error={}", path, pr.exitCode(), pr.stderr());
            return Optional.empty();
        }

        JsonArray array = JsonParser.parseString(pr.stdout()).getAsJsonArray();
        if (array.isEmpty()) return Optional.empty();

        JsonObject obj = array.get(0).getAsJsonObject();
        String rawJson = obj.toString();
        ExifProbeResult exif = GSON.fromJson(obj, ExifProbeResult.class);

        PhotoMetadata metadata = exif.toPhotoMetadata(photoId, rawJson);
        logger.debug("Read metadata for '{}', color style: '{}'", path, metadata.getColorStyle());
        return Optional.of(metadata);
    }

    /**
     * Reads metadata from a batch of files.
     *
     * @param paths      the list of file paths to read metadata from
     * @param isCanceled a supplier to check if the operation is canceled
     * @return a map of file paths to their corresponding photo metadata
     */
    public HashMap<Path, PhotoMetadata> readMetaDataBatch(List<Path> paths, BooleanSupplier isCanceled) {
        List<String> commands = new ExifToolCommandBuilder()
                .addTags(ExifTag.values())
                .asJson()
                .withUtf8()
                .fastMode()
                .addTargets(paths)
                .buildCommands();

        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS * Math.max(1, paths.size() / 20), isCanceled);
        if (!pr.isSuccess() || pr.stdout() == null || pr.stdout().isBlank()) {
            logger.error("Failed to read metadata batch files exitCode={}, error={}", pr.exitCode(), pr.stderr());
            return new HashMap<>();
        }

        HashMap<Path, PhotoMetadata> result = new HashMap<>();
        for (JsonElement el : JsonParser.parseString(pr.stdout()).getAsJsonArray()) {
            JsonObject obj = el.getAsJsonObject();
            ExifProbeResult exif = GSON.fromJson(obj, ExifProbeResult.class);
            result.put(Path.of(exif.sourceFile()).normalize(), exif.toPhotoMetadata(null, obj.toString()));
        }
        return result;
    }

    /**
     * Checks the version of ExifTool.
     *
     * @param isCanceled a supplier to check if the operation is canceled
     * @return an Optional containing the version string if successful, empty otherwise
     */
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

    /**
     * Checks if ExifTool is available.
     *
     * @return true if ExifTool is available, false otherwise
     */
    public boolean isAvailable() {
        boolean available = checkExifToolVersion(null).isPresent();
        logger.debug("ExifTool availability: {}", available);
        return available;
    }

    /**
     * Checks if a file is an image.
     *
     * @param path       the file path to check
     * @param isCanceled a supplier to check if the operation is canceled
     * @return true if the file is an image, false otherwise
     */
    public boolean isImage(Path path, BooleanSupplier isCanceled) {
        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();
        builder.addTags(MIME_TYPE).addTarget(path);
        ArrayList<String> commands = builder.buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, TIMEOUT_MILLIS, isCanceled);
        return pr.stdout().contains("image/");
    }

    /**
     * Probes multiple files and returns their probe results.
     *
     * @param pathList   the list of file paths to probe
     * @param isCanceled a supplier to check if the operation is canceled
     * @return a list of file probe results
     */
    public List<FileProbeResult> probeBatchFiles(List<Path> pathList, BooleanSupplier isCanceled) {
        if (pathList == null || pathList.isEmpty()) return List.of();

        List<FileProbeResult> allResults = new ArrayList<>(pathList.size());

        for (int i = 0; i < pathList.size(); i += CHUNK_SIZE) {
            if (isCanceled != null && isCanceled.getAsBoolean()) break;

            List<Path> chunk = pathList.subList(i, Math.min(i + CHUNK_SIZE, pathList.size()));
            int dynamicTimeout = 5000 + (chunk.size() * 150);

            List<String> commands = new ExifToolCommandBuilder()
                    .asJson()
                    .addTags(FILE_SIZE, FILE_TYPE, IMAGE_SIZE, MIME_TYPE, ORIENTATION)
                    .addTargets(chunk)
                    .buildCommands();

            ProcessResult pr = ProcessRunner.run(commands, dynamicTimeout, isCanceled);
            if (pr.isSuccess() && pr.stdout() != null && !pr.stdout().isBlank()) {
                Type listType = new TypeToken<ArrayList<FileProbeResult>>() {
                }.getType();
                List<FileProbeResult> chunkResults = GSON.fromJson(pr.stdout(), listType);
                allResults.addAll(chunkResults);
            }
        }
        return allResults;
    }
}
