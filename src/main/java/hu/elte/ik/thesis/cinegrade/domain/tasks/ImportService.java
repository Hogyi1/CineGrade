package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.EditState;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.ImportRequest;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import hu.elte.ik.thesis.cinegrade.infra.database.TransactionRunner;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.EditStateDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.MetadataDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.PhotoDao;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.ThumbnailService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Imports staged photos into the catalog in batches of {@link #BATCH_SIZE}.
 * <p>
 * Per photo: place the file (ADD keeps it, COPY copies it, MOVE copies it and deletes the source only
 * after the batch committed), build the {@link PhotoEntry}, attach EXIF metadata. A photo that fails
 * here is skipped, the rest continue.
 * <p>
 * Per batch: photos, metadata and edit states are written in one transaction on the catalog's db
 * thread. If it rolls back, the batch's copied files are deleted and the import continues with the
 * next batch. After a commit the staging thumbnails are stored in the preview database and the batch
 * is handed to {@code onBatchCommitted}.
 * <p>
 * Cancelling stops between photos; every batch committed before that stays imported.
 */
public class ImportService extends Service<List<PhotoEntry>> {

    private static final Logger logger = LogManager.getLogger(ImportService.class);
    private static final int BATCH_SIZE = 25;

    private final List<ImportRequest> requests;
    private final ImportMethod method;
    private final Catalog catalog;
    private final TransactionRunner catalogTx;
    private final PhotoDao photoDao;
    private final MetadataDao metadataDao;
    private final EditStateDao editStateDao;
    private final ThumbnailService thumbnailService;
    private final ExifToolService exifToolService;
    private final StorageService storageService;
    private final Consumer<List<PhotoEntry>> onBatchCommitted;

    public ImportService(List<ImportRequest> requests,
                         ImportMethod method,
                         Catalog catalog,
                         TransactionRunner catalogTx,
                         PhotoDao photoDao,
                         MetadataDao metadataDao,
                         EditStateDao editStateDao,
                         ThumbnailService thumbnailService,
                         ExifToolService exifToolService,
                         StorageService storageService,
                         Consumer<List<PhotoEntry>> onBatchCommitted) {
        this.requests = requests;
        this.method = method;
        this.catalog = catalog;
        this.catalogTx = catalogTx;
        this.photoDao = photoDao;
        this.metadataDao = metadataDao;
        this.editStateDao = editStateDao;
        this.thumbnailService = thumbnailService;
        this.exifToolService = exifToolService;
        this.storageService = storageService;
        this.onBatchCommitted = onBatchCommitted;
    }

    /**
     * One photo waiting in the current batch, with what is needed to commit or undo it.
     */
    private record Pending(PhotoEntry photo, PhotoMetadata metadata, byte[] thumbnail, Path source, Path copy) {
    }

    @Override
    protected Task<List<PhotoEntry>> createTask() {
        return new Task<>() {
            @Override
            protected List<PhotoEntry> call() {
                List<PhotoEntry> allImported = new ArrayList<>();

                List<ImportRequest> valid = requests.stream()
                        .filter(Objects::nonNull)
                        .filter(req -> {
                            boolean exists = req.file() != null && Files.exists(req.file().toPath());
                            if (!exists) {
                                logger.warn("Source file does not exist, skipping: {}", req.file());
                            }
                            return exists;
                        })
                        .toList();

                int total = valid.size();
                logger.info("Starting photo import: {} item(s) via '{}'", total, method);
                updateMessage("Importing photos...");
                updateProgress(0, total);

                String importId = "Import_" + Integer.toHexString(Objects.hash(Instant.now()));
                Path importDirectory = method == ImportMethod.ADD
                        ? null
                        : storageService.ensureDirectory(catalog.getMediaImportDirectory().resolve(importId));

                List<Path> paths = valid.stream().map(ImportRequest::file).map(File::toPath).toList();
                Map<Path, PhotoMetadata> metadataByPath = exifToolService.readMetaDataBatch(paths, this::isCancelled);

                List<Pending> batch = new ArrayList<>();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        break;
                    }

                    ImportRequest request = valid.get(i);
                    updateMessage("Importing photo " + (i + 1) + " of " + total);
                    updateProgress(i + 1, total);

                    Pending pending = prepare(request, importId, importDirectory, metadataByPath);
                    if (pending != null) {
                        batch.add(pending);
                    }

                    if (batch.size() >= BATCH_SIZE) {
                        allImported.addAll(commitBatch(batch));
                        batch.clear();
                    }
                }

                // The last partial batch is committed even when cancelled: its files are already placed
                if (!batch.isEmpty()) {
                    allImported.addAll(commitBatch(batch));
                }

                logger.info("Import finished: {} of {} photo(s) committed", allImported.size(), total);
                return allImported;
            }
        };
    }

    /**
     * Places one file and builds its catalog rows. Returns {@code null} (photo skipped) on any failure.
     */
    private Pending prepare(ImportRequest request, String importId, Path importDirectory, Map<Path, PhotoMetadata> metadataByPath) {
        Path source = request.file().toPath();
        Path copy = null;
        try {
            Path finalPath = source;
            if (importDirectory != null) {
                copy = storageService.atomicCopy(source, uniqueTarget(importDirectory, source.getFileName().toString()));
                finalPath = copy;
            }

            PhotoEntry entry = new PhotoEntry(finalPath.toFile());
            entry.setImportId(importId);
            entry.setImportMethod(method);
            entry.setOriginalPath(source.toAbsolutePath().toString());
            entry.setMimeType(request.mimeType());
            entry.setRaw(request.isRaw());
            entry.setRotation(request.rotation());
            // No staging thumbnail: ThumbnailService.getOrCreate builds it on first view
            entry.setMissingThumbnail(request.thumbnail() == null);

            PhotoMetadata metadata = metadataByPath.get(source.toAbsolutePath().normalize());
            if (metadata == null) {
                metadata = metadataByPath.get(source);
            }
            if (metadata != null) {
                metadata.setPhotoId(entry.getId());
            }

            return new Pending(entry, metadata, request.thumbnail(), source, copy);
        } catch (Exception ex) {
            logger.error("Skipping '{}': could not prepare it for import", source, ex);
            deleteQuietly(copy);
            return null;
        }
    }

    /**
     * Writes one batch in a single transaction, then finishes MOVEs and stores thumbnails.
     *
     * @return the committed photos, or an empty list if the batch was rolled back
     */
    private List<PhotoEntry> commitBatch(List<Pending> batch) {
        List<PhotoEntry> photos = batch.stream().map(Pending::photo).toList();
        List<PhotoMetadata> metas = batch.stream().map(Pending::metadata).filter(Objects::nonNull).toList();
        List<EditState> edits = photos.stream().map(p -> new EditState(p.getId())).toList();

        try {
            catalogTx.execute(c -> {
                photoDao.batchInsert(photos);
                metadataDao.upsertAll(metas);
                editStateDao.upsertAll(edits);
            }).join();
        } catch (CompletionException ex) {
            logger.error("Batch of {} photo(s) rolled back, removing its copied files", batch.size(), ex.getCause());
            batch.forEach(p -> deleteQuietly(p.copy()));
            return List.of();
        }
        logger.debug("Committed batch of {} photo(s)", photos.size());

        // The rows are safe now, so a MOVE may drop its source
        if (method == ImportMethod.MOVE) {
            batch.forEach(p -> deleteQuietly(p.source()));
        }

        // Thumbnails are a cache: if storing them fails the photos are still imported and heal on first view
        Map<String, byte[]> thumbnails = new HashMap<>();
        batch.stream().filter(p -> p.thumbnail() != null).forEach(p -> thumbnails.put(p.photo().getId(), p.thumbnail()));
        try {
            thumbnailService.putAll(thumbnails);
        } catch (Exception ex) {
            logger.warn("Could not store {} thumbnail(s) for the batch", thumbnails.size(), ex);
        }

        if (onBatchCommitted != null) {
            onBatchCommitted.accept(photos);
        }
        return photos;
    }

    /**
     * Picks a free file name in the import directory: name.ext, name_1.ext, name_2.ext, ...
     * Two cards can both hold IMG_0001.CR3, and the second must not overwrite the first.
     */
    private static Path uniqueTarget(Path directory, String fileName) {
        Path target = directory.resolve(fileName);
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        for (int n = 1; Files.exists(target); n++) {
            target = directory.resolve(base + "_" + n + extension);
        }
        return target;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.warn("Could not delete '{}'", path, ex);
        }
    }
}
