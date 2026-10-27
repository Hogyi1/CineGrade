package hu.elte.ik.thesis.cinegrade.app.managers.imports;

import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.PhotoManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.ImportRequest;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageResult;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportService;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportProbeService;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportStageService;
import hu.elte.ik.thesis.cinegrade.infra.database.TransactionRunner;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.EditStateDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.MetadataDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.PhotoDao;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.ThumbnailService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

/**
 * Manages the staged import workflow for photos before they are persisted to the catalog.
 * <p>
 * The staged entries, their filtering and selection live in {@link StagingArea}. This manager
 * coordinates the background task lifecycle for staging (probe, batch staging) and the final
 * import execution.
 */
public class ImportManager implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(ImportManager.class);

    private static final Object IMPORT_GROUP = new Object();

    private final PhotoDao photoDao;
    private final MetadataDao metadataDao;
    private final EditStateDao editStateDao;
    private final TransactionRunner catalogTx;
    private final Catalog catalog;
    private final TaskManager taskManager;
    private final PhotoManager photoManager;
    private final StagingArea stagingArea;
    private final StagingPipeline stagingPipeline;
    private final ThumbnailService thumbnailService;
    private final ExifToolService exifToolService;
    private final StorageService storageService;

    /**
     * Creates a new import manager bound to the given catalog and photo manager.
     *
     * @param catalog      the catalog that will receive imported photos
     * @param taskManager  the task manager used to control background import operations
     * @param photoManager the photo manager that performs the final import into the catalog
     * @param stagingArea  the staging area holding the entries staged for import
     * @param catalogTx    the catalog's transaction runner, owner of all catalog database writes
     */
    public ImportManager(Catalog catalog,
                         TaskManager taskManager,
                         PhotoManager photoManager,
                         StagingArea stagingArea,
                         StagingPipeline stagingPipeline,
                         PhotoDao photoDao,
                         MetadataDao metadataDao,
                         EditStateDao editStateDao,
                         TransactionRunner catalogTx,
                         ThumbnailService thumbnailService,
                         ExifToolService exifToolService,
                         StorageService storageService
                         ) {
        this.exifToolService = exifToolService;
        this.storageService = storageService;
        this.catalog = catalog;
        this.taskManager = taskManager;
        this.photoManager = photoManager;
        this.stagingArea = stagingArea;
        this.stagingPipeline = stagingPipeline;
        this.photoDao = photoDao;
        this.metadataDao = metadataDao;
        this.editStateDao = editStateDao;
        this.catalogTx = catalogTx;
        this.thumbnailService = thumbnailService;

        logger.info("ImportManager initialized for catalog '{}'", catalog != null ? catalog.getCatalogName() : "unknown");
    }

    /**
     * Imports all currently selected staged entries into the catalog and clears the staging area.
     */
    public void importPhotos(ImportMethod method) {
        stagingPipeline.cancel();
        List<ImportRequest> requests = stagingArea.getSelectedEntries().stream()
                .map(ImportRequest::from)
                .toList();

        if (requests.isEmpty()) {
            logger.info("No entries selected for import.");
            return;
        }

        ImportService importService = createImportService(requests, method, catalog);
        logger.info("importPhotos triggered, Currently {} entries selected for import out of {}", requests.size(), stagingArea.getMasterEntries().size());
        importService.start();
    }

    private ImportService createImportService(List<ImportRequest> requests, ImportMethod method, Catalog catalog) {
        photoManager.startImportSession();

        ImportService importService = new ImportService(requests, method, catalog, catalogTx,
                photoDao, metadataDao, editStateDao, thumbnailService, exifToolService, storageService,
                photoManager::addImportedBatch);
        // New type of tasks incoming, these can be stopped from ui
        taskManager.configure(importService, IMPORT_GROUP);

        importService.setOnScheduled(e -> {
            resetAll(); // TODO after importing and trying to import again the import runs
        });

        // The failed batch was already rolled back by TransactionRunner, and TaskManager reports the error
        importService.setOnFailed(e -> logger.error("Background photo import failed", importService.getException()));

        return importService;
    }

    /**
     * Stages the specified files for later import.
     *
     * @param files the files to be staged; if null or empty, no action is performed
     */
    public void stagePhotos(List<File> files) {
        logger.info("stagePhotos called with {} candidate file(s)", files != null ? files.size() : 0);
        if (files == null || files.isEmpty()) {
            return;
        }
        stagingPipeline.stage(files);
    }

    /**
     * Clears all stage entries and related cached data so a new import batch can start cleanly.
     */
    public void resetAll() {
        logger.info("Resetting staging area. Clearing {} staged entries", stagingArea.getMasterEntries().size());
        stagingPipeline.cancel();
        stagingArea.clear();
    }

    @Override
    public void close() throws Exception {
        stagingPipeline.cancel();
        stagingArea.clear();
    }
}
