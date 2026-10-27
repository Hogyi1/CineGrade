package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageResult;
import hu.elte.ik.thesis.cinegrade.domain.enums.PhotoExtensions;
import hu.elte.ik.thesis.cinegrade.infra.utils.file.FileUtils;
import hu.elte.ik.thesis.cinegrade.infra.utils.thumbnail.ThumbnailFactory;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ImportStageService extends Service<StageResult> {

    /**
     * Logger used to report import staging and thumbnail extraction activity.
     */
    private static final Logger logger = LogManager.getLogger(ImportStageService.class);

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

    /**
     * Factory for creating thumbnails.
     */
    private final ThumbnailFactory thumbnailFactory;

    /**
     * Executor service for CPU-bound tasks.
     */
    private final ExecutorService cpuPool;

    /**
     * Creates a service that filters, pairs, and stages probed import files.
     *
     * @param thumbnailFactory creates the staging thumbnails
     * @param cpuPool          pool the thumbnail jobs run on (one job per entry)
     * @param probeResults     results describing the files selected for import
     * @param stageEntries     entries already present in the staging area
     * @param importedPhotos   supplies the photos already imported into the catalog; read on the FX thread here
     */
    public ImportStageService(ThumbnailFactory thumbnailFactory, ExecutorService cpuPool, List<FileProbeResult> probeResults, List<StageEntry> stageEntries, Supplier<List<PhotoEntry>> importedPhotos) {
        this.probeResults = probeResults;
        this.stageEntries = stageEntries;
        this.importedPhotos = importedPhotos.get();
        this.thumbnailFactory = thumbnailFactory;
        this.cpuPool = cpuPool;
    }

    private static boolean isJpeg(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String extension = name.substring(dot + 1).toUpperCase(Locale.ROOT);
        return extension.equals(PhotoExtensions.JPG.getExtension()) || extension.equals(PhotoExtensions.JPEG.getExtension());
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
                .map(p -> FileUtils.getBaseKey(p.getFile()))
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
                                    .set((importedBaseKeys.contains(e.getBaseKey())) ||
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

                if (isCancelled()) {
                    return new StageResult(List.of(), List.of());
                }

                // One job per photo on the cpu pool; a failed thumbnail leaves the entry without one
                List<CompletableFuture<Void>> futures = finalNewEntries.stream()
                        .map(photo -> CompletableFuture
                                .runAsync(() ->
                                {
                                    photo.thumbnail = createThumbnail(photo);
                                    photo.setDecodedThumbnail(new Image(new ByteArrayInputStream(photo.thumbnail),
                                            450,
                                            300,
                                            true,
                                            true));
                                }, cpuPool)
                                .exceptionally(ex -> {
                                    logger.warn("Thumbnail failed for {}", photo.getFile(), ex);
                                    return null;
                                }))
                        .toList();

                // Wait for all of them. Safe: this runs on the task pool, the jobs on the cpu pool
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

                if (isCancelled()) {
                    return new StageResult(List.of(), List.of());
                }

                return new StageResult(finalNewEntries, fxUpdates);
            }
        };
    }

    /**
     * Pairs RAW files with matching JPEG companions and assembles the entries to stage.
     *
     * @param candidates candidate entries identified by the import task
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
                existingStagedByBaseKey.putIfAbsent(FileUtils.getBaseKey(s.getFile()), s);
            }
        }

        // Map current batch JPEGs by base key
        Map<String, StageEntry> batchJpegsByBaseKey = new HashMap<>();
        for (StageEntry j : jpegs) {
            batchJpegsByBaseKey.putIfAbsent(FileUtils.getBaseKey(j.getFile()), j);
        }

        List<StageEntry> resultList = new ArrayList<>();
        Set<String> claimedBatchJpegPaths = new HashSet<>();
        // folder -> (base name -> JPEG), filled on first use so each folder is listed once per batch
        Map<Path, Map<String, File>> diskJpegsByFolder = new HashMap<>();

        // 1. Process RAW candidates
        for (StageEntry raw : raws) {
            String baseKey = FileUtils.getBaseKey(raw.getFile());

            // Look for companion JPEG:
            // Case A: Companion JPEG is in the current batch
            StageEntry batchJpeg = batchJpegsByBaseKey.get(baseKey);
            if (batchJpeg != null) {
                raw.jpegPath = batchJpeg.getFile().toPath();
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
                    raw.rotation = stagedJpeg.rotation;
                    fxUpdates.add(() -> stagedJpeg.getIsCompanionProperty().set(true));
                } else {
                    // Case C: Companion JPEG sits on disk next to the RAW
                    File diskJpeg = findDiskCompanionJpeg(raw.getFile(), diskJpegsByFolder);
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
                resultList.add(jpeg);
                continue;
            }

            // If a RAW with the same base key was already staged previously, attach this JPEG as companion
            String baseKey = FileUtils.getBaseKey(jpeg.getFile());
            StageEntry stagedRaw = existingStagedByBaseKey.get(baseKey);
            if (stagedRaw != null && stagedRaw.isRaw) {
                fxUpdates.add(() -> {
                    stagedRaw.jpegPath = jpeg.getFile().toPath();
                    stagedRaw.companion = jpeg;
                });
                jpeg.getIsCompanionProperty().set(true);   // new entry, not in the list yet, fine
                resultList.add(jpeg);
                continue;
            }

            // Standalone
            resultList.add(jpeg);
        }

        return resultList;
    }

    /**
     * Creates the thumbnail of a staged entry. A RAW without a usable embedded preview falls back to
     * its JPEG companion, if it has one.
     *
     * @param entry the staged entry
     * @return the thumbnail encoded as JPEG
     */
    private byte[] createThumbnail(StageEntry entry) {
        if (!entry.isRaw) {
            return thumbnailFactory.fromImage(entry.getFile(), entry.rotation);
        }
        try {
            return thumbnailFactory.fromRaw(entry.getFile(), entry.rotation);
        } catch (RuntimeException ex) {
            if (entry.jpegPath == null) {
                throw ex;
            }
            logger.debug("No embedded preview in '{}', using companion '{}'", entry.getFile().getName(), entry.jpegPath.getFileName());
            return thumbnailFactory.fromImage(entry.jpegPath.toFile(), entry.rotation);
        }
    }

    /**
     * Finds the JPEG next to the RAW file with the same name excluding its extension.
     * Only .jpg/.jpeg files count, so sidecars like IMG_0001.xmp are never picked up.
     *
     * @param rawFile       RAW file for which to search for a companion
     * @param jpegsByFolder per-batch cache of each folder's JPEGs, keyed by lower-case base name
     * @return the matching file, or {@code null} if none is found or the directory cannot be read
     */
    private File findDiskCompanionJpeg(File rawFile, Map<Path, Map<String, File>> jpegsByFolder) {
        if (rawFile == null || rawFile.getParentFile() == null) {
            return null;
        }
        Path folder = rawFile.getParentFile().toPath();
        Map<String, File> jpegs = jpegsByFolder.computeIfAbsent(folder, this::listJpegs);
        return jpegs.get(FileUtils.getFileNameWithoutExtension(rawFile));
    }

    /**
     * Lists the JPEG files of a folder once.
     *
     * @param folder the folder to list
     * @return lower-case base name to JPEG file; empty if the folder cannot be read
     */
    private Map<String, File> listJpegs(Path folder) {
        Map<String, File> jpegs = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (isJpeg(path) && Files.isRegularFile(path)) {
                    jpegs.putIfAbsent(FileUtils.getFileNameWithoutExtension(path.toFile()), path.toFile());
                }
            }
        } catch (IOException e) {
            logger.error("Error occurred while listing '{}' for companion JPEG files", folder, e);
        }
        return jpegs;
    }
}