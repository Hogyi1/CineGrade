package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.EditState;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.ImportRequest;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibrawService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.EditStateDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.MetadataDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.PhotoDao;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.coobird.thumbnailator.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ImportService extends Service<List<PhotoEntry>> {

    private static final Logger logger = LogManager.getLogger(ImportService.class);
    private static final int BATCH_SIZE = 25;
    private static final int REQ_WIDTH = 1280, REQ_HEIGHT = 720;
    private static final double QUALITY = 0.75;
    private final List<ImportRequest> stageEntries;
    private final ImportMethod method;
    private final Catalog catalog;
    private final Connection connection;
    private final StorageService storageService;
    private final ExifToolService exifToolService;
    private final PhotoDao photoDao;
    private final EditStateDao editStateDao;
    private final MetadataDao metadataDao;
    private final Consumer<List<PhotoEntry>> onBatchCommitted;

    public ImportService(List<ImportRequest> stageEntries, ImportMethod method, Catalog catalog, Connection connection, Consumer<List<PhotoEntry>> onBatchCommitted) {
        this.stageEntries = stageEntries;
        this.method = method;
        this.catalog = catalog;
        this.connection = connection;
        this.onBatchCommitted = onBatchCommitted;

        this.storageService = new StorageService(catalog.getProjectDirectory());
        this.photoDao = new PhotoDao(connection, catalog.getProjectDirectory());
        this.editStateDao = new EditStateDao(connection);
        this.metadataDao = new MetadataDao(connection);
        this.exifToolService = new ExifToolService();
    }

    @Override
    protected Task<List<PhotoEntry>> createTask() {
        return new Task<>() {
            @Override
            protected List<PhotoEntry> call() throws Exception {
                List<PhotoEntry> allImported = new ArrayList<>();

                // Filter non-null and existing source files
                List<ImportRequest> validRequests = stageEntries.stream()
                        .filter(Objects::nonNull)
                        .filter(req -> {
                            boolean exists = req.file() != null && Files.exists(req.file().toPath());
                            if (!exists) {
                                logger.warn("Source file does not exist, skipping: {}", req.file());
                            }
                            return exists;
                        })
                        .toList();

                int totalSteps = validRequests.size();
                logger.info("Starting progressive photo import: {} items via '{}'", totalSteps, method);
                updateMessage("Importing photos...");
                updateProgress(0, totalSteps);

                String currentImportDirectory = "Import_" + Integer.toHexString(Objects.hash(Instant.now()));
                Path mediaImportsDirectory = storageService.ensureDirectory(catalog.getMediaImportDirectory().resolve(currentImportDirectory));
                Path thumbnailDirectory = storageService.ensureDirectory(catalog.getThumbnailsDirectory().resolve(currentImportDirectory));

                // Batch EXIF probe
                List<Path> importPaths = validRequests.stream().map(ImportRequest::file).map(File::toPath).toList();
                HashMap<Path, PhotoMetadata> metadataMap = exifToolService.readMetaDataBatch(importPaths, this::isCancelled);

                List<PhotoEntry> currentBatchPhotos = new ArrayList<>();
                List<PhotoMetadata> currentBatchMeta = new ArrayList<>();
                List<EditState> currentBatchEdits = new ArrayList<>();

                try (LibrawService librawService = new LibrawService()) {
                    for (int i = 0; i < validRequests.size(); i++) {
                        if (isCancelled()) {
                            connection.rollback();
                            return allImported;
                        }

                        ImportRequest request = validRequests.get(i);
                        String fileName = request.file().getName();
                        updateMessage("Importing photo " + (i + 1) + " of " + totalSteps);
                        updateProgress(i + 1, totalSteps);

                        // 1. Move or Copy file if needed
                        Path destinationFile = mediaImportsDirectory.resolve(fileName);
                        Path finalFilePath = useImportMethod(destinationFile, request);

                        // 2. Create PhotoEntry (absolute File in memory)
                        PhotoEntry entry = new PhotoEntry(finalFilePath.toFile());
                        entry.setImportId(currentImportDirectory);
                        entry.setImportMethod(method);
                        entry.setOriginalPath(request.file().getAbsolutePath());

                        // 3. Generate thumbnail
                        String hash = Integer.toHexString(request.file().getAbsolutePath().hashCode());
                        String thumbnailName = hash + "_" + fileName + "_thumb.jpg";
                        Path targetThumbPath = thumbnailDirectory.resolve(thumbnailName);

                        boolean thumbSuccess = false;
                        if (request.isRaw()) {
                            thumbSuccess = librawService.extractThumbnail(finalFilePath.toFile(), targetThumbPath);
                            if (!thumbSuccess && request.jpegPath() != null) {
                                thumbSuccess = downScaleJpeg(request.jpegPath(), targetThumbPath);
                            }
                        } else {
                            thumbSuccess = downScaleJpeg(finalFilePath, targetThumbPath);
                        }

                        if (thumbSuccess) {
                            entry.setThumbnailPath(targetThumbPath.toString());
                        } else {
                            entry.setMissingThumbnail(true);
                        }

                        // 4. Attach EXIF metadata
                        Path lookupKey = request.file().toPath().toAbsolutePath().normalize();
                        PhotoMetadata metadata = metadataMap.get(lookupKey);
                        if (metadata == null) {
                            metadata = metadataMap.get(request.file().toPath());
                        }
                        if (metadata != null) {
                            metadata.setPhotoId(entry.getId());
                            currentBatchMeta.add(metadata);
                        }

                        currentBatchEdits.add(new EditState(entry.getId()));
                        currentBatchPhotos.add(entry);
                        allImported.add(entry);

                        // 5. Commit chunk if batch size reached or at end
                        if (currentBatchPhotos.size() >= BATCH_SIZE || i == validRequests.size() - 1) {
                            commitBatch(currentBatchPhotos, currentBatchMeta, currentBatchEdits);
                            if (onBatchCommitted != null) {
                                List<PhotoEntry> emitted = new ArrayList<>(currentBatchPhotos);
                                onBatchCommitted.accept(emitted);
                            }
                            currentBatchPhotos.clear();
                            currentBatchMeta.clear();
                            currentBatchEdits.clear();
                        }
                    }
                }

                logger.info("Import completed successfully: {} photo(s) committed", allImported.size());
                return allImported;
            }

            private void commitBatch(List<PhotoEntry> photos, List<PhotoMetadata> metas, List<EditState> edits) throws SQLException {
                boolean prevAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    photoDao.batchInsert(photos);
                    if (!metas.isEmpty()) metadataDao.upsertAll(metas);
                    if (!edits.isEmpty()) editStateDao.upsertAll(edits);
                    connection.commit();
                    logger.debug("Committed progressive batch of {} photo(s)", photos.size());
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(prevAutoCommit);
                }
            }
        };
    }

    /**
     * Uses the appropriate import method to handle the file transfer.
     *
     * @param targetPath the path where the file should be moved/copied
     * @param request    the import request containing file information
     * @return the path of the imported file
     */
    private Path useImportMethod(Path targetPath, ImportRequest request) {
        switch (method) {
            case COPY -> {
                logger.debug("Copying file '{}' -> '{}'", request.file().getAbsolutePath(), targetPath);
                return storageService.atomicCopy(request.file().toPath(), targetPath);
            }
            case MOVE -> {
                logger.debug("Moving file '{}' -> '{}'", request.file().getAbsolutePath(), targetPath);
                return storageService.atomicMove(request.file().toPath(), targetPath);
            }
            default -> {
                logger.debug("Retaining file in-place (ADD method): '{}'", request.file().getAbsolutePath());
                return request.file().toPath();
            }
        }
    }

    private boolean downScaleJpeg(Path sourcePath, Path targetPath) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            logger.warn("Cannot downscale JPEG: source file does not exist ({})", sourcePath);
            return false;
        }
        try {
            storageService.ensureDirectory(targetPath.getParent());
            logger.debug("Downscaling JPEG thumbnail: '{}' -> '{}' ({}x{}, quality={})",
                    sourcePath.getFileName(), targetPath.getFileName(), REQ_WIDTH, REQ_HEIGHT, QUALITY);
            Thumbnails.of(sourcePath.toFile())
                    .size(REQ_WIDTH, REQ_HEIGHT)
                    .outputQuality(QUALITY)
                    .allowOverwrite(true)
                    .toFile(targetPath.toFile());
            return true;
        } catch (IOException ex) {
            logger.error("Failed to generate thumbnail for: {}", sourcePath, ex);
            return false;
        }
    }
}
