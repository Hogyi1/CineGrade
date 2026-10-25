package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageResult;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibrawService;
import hu.elte.ik.thesis.cinegrade.infra.services.StorageService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ImportStageService extends Service<StageResult> {

    /**
     * Logger used to report import staging and thumbnail extraction activity.
     */
    private static final Logger logger = LogManager.getLogger(ImportStageService.class);

    /**
     * Session directory used to store extracted thumbnails.
     */
    private final Path sessionDirectory;

    /**
     * Entries already staged before this service runs.
     */
    private final List<StageEntry> stageEntries;

    /**
     * Photos already present in the catalog.
     */
    private final List<PhotoEntry> importedPhotos;

    /**
     * Results from probing the files considered for import.
     */
    private final List<FileProbeResult> probeResults;

    private final StorageService storageService;

    /**
     * Creates a service that filters, pairs, and stages probed import files.
     *
     * @param probeResults   results describing the files selected for import
     * @param stageEntries   entries already present in the staging area
     * @param importedPhotos photos already imported into the catalog
     * @param sessionDirectory session directory used for import-related storage
     */
    public ImportStageService(List<FileProbeResult> probeResults, List<StageEntry> stageEntries, List<PhotoEntry> importedPhotos, Path sessionDirectory) {
        this.probeResults = probeResults;
        this.stageEntries = stageEntries;
        this.importedPhotos = importedPhotos;
        this.sessionDirectory = sessionDirectory;
        this.storageService = new StorageService(sessionDirectory);
    }

    /**
     * Builds a normalized key from a file's absolute path without its extension.
     *
     * @param file file whose path should be normalized
     * @return the lower-case path without its final extension, or an empty string if {@code file} is null
     */
    private static String getBaseKey(File file) {
        if (file == null) {
            return "";
        }
        String path = file.getAbsolutePath();
        int dotIndex = path.lastIndexOf('.');
        int sepIndex = Math.max(path.lastIndexOf(File.separatorChar), path.lastIndexOf('/'));
        return (dotIndex > sepIndex ? path.substring(0, dotIndex) : path).toLowerCase(Locale.ROOT);
    }

    /**
     * Gets a file's lower-case name without its extension.
     *
     * @param file file whose name should be normalized
     * @return the lower-case name without its final extension, or an empty string if {@code file} is null
     */
    private static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return "";
        }
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0 ? name.substring(0, dotIndex) : name).toLowerCase(Locale.ROOT);
    }

    /**
     * Creates the background task that filters probed files, pairs companions, and prepares thumbnails.
     *
     * @return a task that produces the new staged entries
     */
    @Override
    protected Task<StageResult> createTask() {
        Set<String> importedExactPaths = importedPhotos.stream()
                .filter(Objects::nonNull)
                .map(p -> p.getFile().getAbsolutePath().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> importedBaseKeys = importedPhotos.stream()
                .filter(Objects::nonNull)
                .map(p -> getBaseKey(p.getFile()))
                .collect(Collectors.toSet());

        Set<String> importedFileNamesWithoutExtension = importedPhotos.stream()
                .filter(Objects::nonNull)
                .map(p -> getFileNameWithoutExtension(p.getFile()))
                .collect(Collectors.toSet());

        Set<String> importedFileNames = importedPhotos.stream()
                .filter(Objects::nonNull)
                .map(p -> p.getFile().getName())
                .collect(Collectors.toSet());

        Set<String> importedOriginalPaths = importedPhotos.stream()
                .filter(Objects::nonNull)
                .map(p -> p.getOriginalPath().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> existingStagedPaths = stageEntries.stream()
                .filter(Objects::nonNull)
                .map(s -> s.getFile().getAbsolutePath().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> stagedFileNames = stageEntries.stream()
                .filter(Objects::nonNull)
                .map(s -> s.getFile().getName())
                .collect(Collectors.toSet());

        List<StageEntry> stagedSnapshot = new ArrayList<>(stageEntries);

        return new Task<>() {

            /**
             * Filters duplicate files, pairs RAW and JPEG files, and attaches missing thumbnails.
             *
             * @return the entries newly staged by this task
             * @throws Exception if the background task encounters an error
             */
            @Override
            protected StageResult call() throws Exception {
                logger.debug("ImportStageService running with {} probe result(s), {} existing staged, {} imported catalog photos",
                        probeResults != null ? probeResults.size() : 0, existingStagedPaths.size(), importedExactPaths.size());

                // Filter nulls and images only
                List<FileProbeResult> imageResults = probeResults.stream().filter(Objects::nonNull).filter(FileProbeResult::isImage).toList();

                // 2. Filter image files and perform Database Duplicate Checking
                List<StageEntry> candidates = imageResults.stream()
                        .filter(result -> {
                            File file = result.toFile();
                            String exactPath = file.getAbsolutePath().toLowerCase(Locale.ROOT);
                            return (!importedExactPaths.contains(exactPath) &&
                                    !existingStagedPaths.contains(exactPath) &&
                                    !importedOriginalPaths.contains(exactPath));
                        })
                        .map(result -> {
                            StageEntry e = new StageEntry(result);
                            e.getIsMaybeDuplicateProperty()
                                    .set( (importedBaseKeys.contains(e.getBaseKey())) ||
                                            importedFileNames.contains(e.getFile().getName()) ||
                                            (stagedFileNames.contains(e.getFile().getName())));
                            return e;
                        })
                        .toList();

                logger.debug("Filtered {} probe result(s) to {} image(s) and {} candidate(s) for staging",
                        probeResults != null ? probeResults.size() : 0, imageResults.size(), candidates.size());

                // 3. Staging Deduplication & Companion Pairing
                List<Runnable> fxUpdates = new ArrayList<>();
                List<StageEntry> finalNewEntries = pairAndStage(candidates, stagedSnapshot, fxUpdates);
                logger.debug("pairAndStage produced {} new staged entries", finalNewEntries.size());

                // 4. Attach thumbnails for entries that need one (RAWs without JPEG companions)
                List<StageEntry> needsThumb = finalNewEntries.stream()
                        .filter(e -> e.thumbnailPath == null)
                        .toList();

                if (!needsThumb.isEmpty()) {
                    logger.debug("Extracting thumbnails for {} entries", needsThumb.size());
                }

                needsThumb.parallelStream().forEach(ImportStageService.this::attachThumbnail);

                return new StageResult(finalNewEntries, fxUpdates);
            }
        };
    }

    /**
     * Pairs RAW files with matching JPEG companions and assembles the entries to stage.
     *
     * @param candidates          candidate entries identified by the import task
     * @return entries to stage, including companion JPEG entries
     */
    private List<StageEntry> pairAndStage(List<StageEntry> candidates, List<StageEntry> stagedSnapshot, List<Runnable> fxUpdates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<StageEntry> raws = candidates.stream().filter(e -> e.isRaw).toList();
        List<StageEntry> jpegs = candidates.stream().filter(e -> !e.isRaw).toList();

        // Map existing staged entries by lowercase base key
        Map<String, StageEntry> existingStagedByBaseKey = new HashMap<>();
        for (StageEntry s : stagedSnapshot) {
            if (s != null && s.getFile() != null) {
                existingStagedByBaseKey.putIfAbsent(getBaseKey(s.getFile()), s);
            }
        }

        // Map current batch JPEGs by base key
        Map<String, StageEntry> batchJpegsByBaseKey = new HashMap<>();
        for (StageEntry j : jpegs) {
            batchJpegsByBaseKey.putIfAbsent(getBaseKey(j.getFile()), j);
        }

        List<StageEntry> resultList = new ArrayList<>();
        Set<String> claimedBatchJpegPaths = new HashSet<>();

        // 1. Process RAW candidates
        for (StageEntry raw : raws) {
            String baseKey = getBaseKey(raw.getFile());

            // Look for companion JPEG:
            // Case A: Companion JPEG is in the current batch
            StageEntry batchJpeg = batchJpegsByBaseKey.get(baseKey);
            if (batchJpeg != null) {
                raw.jpegPath = batchJpeg.getFile().toPath();
                raw.thumbnailPath = batchJpeg.getFile().toPath();
                raw.companion = batchJpeg;
                raw.rotation = batchJpeg.rotation;
                batchJpeg.getIsCompanionProperty().set(true);
                claimedBatchJpegPaths.add(batchJpeg.getFile().getAbsolutePath().toLowerCase(Locale.ROOT));
            } else {
                // Case B: Companion JPEG was staged previously
                StageEntry stagedJpeg = existingStagedByBaseKey.get(baseKey);
                if (stagedJpeg != null && !stagedJpeg.isRaw) {
                    raw.jpegPath = stagedJpeg.getFile().toPath();
                    raw.companion = stagedJpeg;
                    raw.rotation = stagedJpeg.thumbnailPath != null ? stagedJpeg.rotation : raw.rotation;
                    raw.thumbnailPath = stagedJpeg.thumbnailPath != null ? stagedJpeg.thumbnailPath : stagedJpeg.getFile().toPath();
                    fxUpdates.add(() -> stagedJpeg.getIsCompanionProperty().set(true));
                } else {
                    // Case C: Companion JPEG sits on disk next to the RAW
                    File diskJpeg = findDiskCompanionJpeg(raw.getFile());
                    if (diskJpeg != null) {
                        raw.jpegPath = diskJpeg.toPath();
                    }
                }
            }

            resultList.add(raw);
        }

        // 2. Process JPEG candidates
        for (StageEntry jpeg : jpegs) {
            String jpegPath = jpeg.getFile().getAbsolutePath().toLowerCase(Locale.ROOT);

            // If claimed by a RAW in the current batch, keep it as a companion
            if (claimedBatchJpegPaths.contains(jpegPath)) {
                jpeg.getIsCompanionProperty().set(true);
                jpeg.thumbnailPath = jpeg.getFile().toPath();
                resultList.add(jpeg);
                continue;
            }

            // If a RAW with the same base key was already staged previously, attach this JPEG as companion
            String baseKey = getBaseKey(jpeg.getFile());
            StageEntry stagedRaw = existingStagedByBaseKey.get(baseKey);
            if (stagedRaw != null && stagedRaw.isRaw) {
                fxUpdates.add(() -> {
                    stagedRaw.jpegPath = jpeg.getFile().toPath();
                    stagedRaw.companion = jpeg;
                    if (stagedRaw.thumbnailPath == null) {
                        stagedRaw.thumbnailPath = jpeg.getFile().toPath();
                        stagedRaw.rotation = jpeg.rotation;
                    }
                });
                jpeg.getIsCompanionProperty().set(true);   // new entry, not in the list yet, fine
                jpeg.thumbnailPath = jpeg.getFile().toPath();
                resultList.add(jpeg);
                continue;
            }

            // Standalone
            resultList.add(jpeg);
        }

        return resultList;
    }

    /**
     * Finds a regular file in the RAW file's directory with the same name excluding its extension.
     *
     * @param rawFile RAW file for which to search for a companion
     * @return the matching file, or {@code null} if none is found or the directory cannot be read
     */
    private File findDiskCompanionJpeg(File rawFile) {
        if (rawFile == null || rawFile.getParentFile() == null) {
            return null;
        }
        File parent = rawFile.getParentFile();
        String baseName = getFileNameWithoutExtension(rawFile);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent.toPath())) {
            for (Path path : stream) {
                if (!path.toFile().equals(rawFile) &&
                        getFileNameWithoutExtension(path.toFile()).equals(baseName) &&
                        Files.isRegularFile(path)) {
                    return path.toFile();
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred while searching for companion JPEG file", e);
        }
        return null;
    }

    /**
     * Assigns a cached or newly extracted thumbnail to a staged entry.
     *
     * @param entry entry whose thumbnail should be attached
     */
    private void attachThumbnail(StageEntry entry) {

        String hash = Integer.toHexString(entry.getFile().getAbsolutePath().hashCode());
        String thumbnailName = hash + "_" + entry.getFile().getName() + "_thumb.jpg";
        Path targetPath = sessionDirectory.resolve(thumbnailName);

        if (Files.exists(targetPath)) {
            try {
                if (Files.size(targetPath) > 0) {
                    entry.thumbnailPath = targetPath;
                    logger.debug("Reusing cached thumbnail for: '{}'", entry.getFile().getName());
                    return;
                }
            } catch (IOException ignored) {
            }
        }

        if (entry.isRaw) {
            try (LibrawService librawService = new LibrawService()) {
                logger.debug("Extracting thumbnail for entry: '{}' -> '{}'", entry.getFile().getName(), targetPath);
                boolean isSuccess = librawService.extractThumbnail(entry.getFile(), targetPath);
                if (isSuccess) {
                    entry.thumbnailPath = targetPath;
                    logger.debug("Thumbnail successfully extracted for: '{}'", entry.getFile().getName());
                } else {
                    logger.error("Failed to extract thumbnail for entry: {}", entry.getFile().getName());
                    entry.thumbnailPath = null;
                }
            } catch (Exception e) {
                logger.error("Error occurred while extracting thumbnail for entry: {}", entry.getFile().getName(), e);
                entry.thumbnailPath = null;
            }
        } else {
            if (downScaleJpeg(entry.getFile().toPath(), targetPath)) {
                entry.thumbnailPath = targetPath;
                return;
            }
            entry.thumbnailPath = null;
        }
    }

    private boolean downScaleJpeg(Path sourcePath, Path targetPath) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            logger.warn("Cannot downscale JPEG: source file does not exist ({})", sourcePath);
            return false;
        }
        try {
            storageService.ensureDirectory(targetPath.getParent());
            logger.debug("Downscaling thumbnail: '{}' -> '{}' ({}x{}, quality={})",
                    sourcePath.getFileName(), targetPath.getFileName(), 450, 300, 0.8);
            Thumbnails.of(sourcePath.toFile())
                    .size(450, 300)
                    .outputQuality(0.8)
                    .allowOverwrite(true)
                    .toFile(targetPath.toFile());
            return true;
        } catch (IOException ex) {
            logger.error("Failed to generate thumbnail for: {}", sourcePath, ex);
            return false;
        }
    }
}