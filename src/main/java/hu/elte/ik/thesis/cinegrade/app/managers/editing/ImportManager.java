package hu.elte.ik.thesis.cinegrade.app.managers.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
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
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
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
 * The manager tracks candidate files that are being probed, keeps the current stage list,
 * exposes selection state for import, and coordinates the background task lifecycle for
 * import preparation and final import execution.
 */
public class ImportManager implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(ImportManager.class);

    private static final int BATCH_COUNT = 25;
    private static final Object STAGE_GROUP = new Object();
    private static final Object IMPORT_GROUP = new Object();

    // Look at those properties and fire a change when these change inside
    private final ObservableList<StageEntry> masterEntries = FXCollections.observableArrayList(e -> new Observable[]{e.getIsSelectedProperty(), e.getIsMaybeDuplicateProperty(), e.getIsCompanionProperty()});

    // Filter on different predicates
    private final FilteredList<StageEntry> filteredEntries = new FilteredList<>(masterEntries);
    // Sort on filenames
    private final SortedList<StageEntry> sortedEntries = new SortedList<>(filteredEntries);

    private final LongProperty stageSizeInBytes = new SimpleLongProperty(0);
    private final IntegerProperty selectedFilesProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty stagedFilesProperty = new SimpleIntegerProperty(0);

    private final BooleanProperty autoPairProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty hideDuplicatesProperty = new SimpleBooleanProperty(true);
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final BooleanProperty stagingProperty = new SimpleBooleanProperty(false);

    private final Connection connection;
    private final Catalog catalog;
    private final TaskManager taskManager;
    private final PhotoManager photoManager;
    private final ExifToolService exifToolService;
    private final StorageService storageService;

    // private final ChangeListener<Boolean> selectionListener;
    private final ArrayDeque<List<FileProbeResult>> pendingBatches = new ArrayDeque<>();
    private Path sessionDirectory;


    /**
     * Creates a new import manager bound to the given catalog and photo manager.
     *
     * @param connection   the database connection associated with the current application context
     * @param catalog      the catalog that will receive imported photos
     * @param taskManager  the task manager used to control background import operations
     * @param photoManager the photo manager that performs the final import into the catalog
     */
    public ImportManager(Connection connection, Catalog catalog, TaskManager taskManager, PhotoManager photoManager) {
        exifToolService = new ExifToolService();
        storageService = new StorageService(catalog.getTempDirectory());
        this.catalog = catalog;
        this.taskManager = taskManager;
        this.photoManager = photoManager;
        this.connection = connection;

        logger.info("ImportManager initialized for catalog '{}'", catalog != null ? catalog.getCatalogName() : "unknown");

        filteredEntries.addListener((ListChangeListener<StageEntry>) change -> {
            logger.debug("FilteredEntries changed: count = {}, masterEntries count = {}", filteredEntries.size(), masterEntries.size());
            calculateStageSize();
            updateSelectedCount();
        });

        stagedFilesProperty.bind(Bindings.size(filteredEntries));
        sortedEntries.setComparator(Comparator.comparing(StageEntry::getFileNameWithoutExtension)
                .thenComparing((StageEntry e) -> e.isRaw ? 0 : 1));
//        selectionListener = (observable, oldValue, newValue) -> {
//            if (newValue != oldValue) {
//                updateSelectedCount();
//            }
//        };

        filteredEntries.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            boolean hideCompanions = autoPairProperty.get();
            boolean hideDuplicates = hideDuplicatesProperty.get();
            String nameFilter = nameProperty.get();

            return (Predicate<StageEntry>) entry -> {
                boolean matchesName = nameFilter.isEmpty() || entry.getFile().getName().toLowerCase().contains(nameFilter.toLowerCase());
                boolean isCompanion = !hideCompanions || !entry.getIsCompanionProperty().get();
                boolean isDuplicate = !hideDuplicates || !entry.getIsMaybeDuplicateProperty().get();

                return matchesName && isCompanion && isDuplicate;
            };
        }, autoPairProperty, hideDuplicatesProperty, nameProperty));

        createSessionDirectory();
    }

    /**
     * Imports all currently selected staged entries into the catalog and clears the staging area.
     */
    public void importPhotos(ImportMethod method) {
        cancelStaging();
        List<ImportRequest> requests = filteredEntries.stream()
                .filter(entry -> entry.getIsSelectedProperty().get())
                .map(ImportRequest::from)
                .toList();

        if (requests.isEmpty()) {
            logger.info("No entries selected for import.");
            return;
        }

        ImportService importService = createImportService(requests, method, catalog);
        logger.info("importPhotos triggered, Currently {} entries selected for import out of {}", requests.size(), masterEntries.size());
        importService.start();
    }

    private ImportService createImportService(List<ImportRequest> requests, ImportMethod method, Catalog catalog) {
        photoManager.startImportSession();

        ImportService importService = new ImportService(requests, method, catalog, connection, photoManager::addImportedBatch);
        // New type of tasks incoming, these can be stopped from ui
        taskManager.configure(importService, IMPORT_GROUP);

        importService.setOnSucceeded(e -> {
            List<PhotoEntry> photoEntries = importService.getValue();
            photoManager.addAll(photoEntries);
            resetAll();
        });

        importService.setOnFailed(e -> {
            Throwable exception = importService.getException();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, "Background photo import failed", ex);
            }
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Background photo import failed", exception);
        });

        return importService;
    }

    /**
     * Starts probing and staging the supplied files for later import.
     *
     * @param files the files to be analyzed and staged; if null or empty, no action is performed
     */
    public void stagePhotos(List<File> files) {
        logger.info("stagePhotos called with {} candidate file(s)", files != null ? files.size() : 0);
        if (files == null || files.isEmpty()) {
            return;
        }

        ImportProbeService stagingService = createStagingServices(exifToolService, files);
        stagingService.start();
    }

    /**
     * Creates the asynchronous probe task responsible for scanning the given files.
     *
     * @param exifToolService the ExifTool service used to inspect file metadata
     * @param files           the list of files to probe
     * @return the configured probe service that will emit file metadata results
     */
    private ImportProbeService createStagingServices(ExifToolService exifToolService, List<File> files) {
        stagingProperty.set(true);

        ImportProbeService probeService = new ImportProbeService(exifToolService, files);
        taskManager.configure(probeService, STAGE_GROUP);

        probeService.setOnSucceeded(e -> {
            List<FileProbeResult> result = probeService.getValue();
            logger.debug("Probe completed with {} result(s), dispatching staging tasks in batches of {}", result != null ? result.size() : 0, BATCH_COUNT);

            for (int i = 0; i < result.size(); i += BATCH_COUNT) {
                pendingBatches.add(result.subList(i, Math.min(i + BATCH_COUNT, result.size())));
            }
            stageNextBatch(sessionDirectory);
        });

        probeService.setOnFailed(e -> {
            stagingProperty.set(false);
            Throwable ex = probeService.getException();
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Background photo probing failed", ex);
        });
        return probeService;
    }

    /**
     * Stages the next batch of file probe results.
     *
     * @param session the session directory for staging
     */
    private void stageNextBatch(Path session) {
        List<FileProbeResult> batch = pendingBatches.poll();
        if (batch == null) {
            stagingProperty.set(false);
            return;
        }

        ImportStageService service =
                new ImportStageService(batch, masterEntries, photoManager.getPhotos(), session);
        taskManager.configure(service, STAGE_GROUP);

        service.setOnSucceeded(e -> {
            if (!session.equals(sessionDirectory)) return;
            StageResult result = service.getValue();
            result.fxUpdates().forEach(Runnable::run);

            if (result.entries() != null && !result.entries().isEmpty()) {
                masterEntries.addAll(result.entries());
            }

            logger.info("Added {} entries. Total staged: {}", result.entries().size(), masterEntries.size());
            stageNextBatch(session);
        });

        service.setOnFailed(e -> {
            if (pendingBatches.peek() == null) stagingProperty.set(false);
            stageNextBatch(session);
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Background photo staging failed", service.getException());
        });

        service.start();
    }

    /**
     * Creates the session directory for the import process.
     */
    private void createSessionDirectory() {
        String id = Integer.toHexString(Objects.hash("Import_" + Instant.now()));
        Path dir = catalog.getTempImportsDirectory().resolve(id);
        storageService.ensureDirectory(dir);
        sessionDirectory = dir;
    }

    /**
     * Updates the count of selected files.
     */
    private void updateSelectedCount() {
        Platform.runLater(() -> selectedFilesProperty.set((int) filteredEntries.stream().filter(entry -> entry.getIsSelectedProperty().get()).count()));
    }

    /**
     * Updates the selection state for every entry currently visible in the filtered list.
     *
     * @param isSelected true to select all visible staged entries, false to clear their selection
     */
    public void selectAll(boolean isSelected) {
        logger.info("Setting selection state for all {} staged entries to: {}", filteredEntries.size(), isSelected);
        filteredEntries.forEach(entry -> entry.getIsSelectedProperty().set(isSelected));
    }

    /**
     * Cancels the current staging process.
     */
    public void cancelStaging() {
        pendingBatches.clear();
        taskManager.cancelGroup(STAGE_GROUP);
    }

    /**
     * Clears the thumbnail cache directory associated with the current catalog imports.
     */
    private void deleteThumbnailCache() {
        storageService.cleanDirectory(catalog.getTempImportsDirectory());
    }

    /**
     * Recalculates the total size of all currently staged files in bytes.
     */
    private void calculateStageSize() {
        long byteSize = filteredEntries.stream().filter(entry -> entry.getIsSelectedProperty().get()).mapToLong(entry -> entry.size).sum();
        Platform.runLater(() -> stageSizeInBytes.set(byteSize));
        logger.debug("Stage size updated: {} bytes across {} staged entries", byteSize, masterEntries.size());
    }

    /**
     * Returns the filtered list of staged entries visible to the current view.
     *
     * @return the filtered list of staged import entries
     */
    public SortedList<StageEntry> getFilteredEntries() {
        return sortedEntries;
    }

    /**
     * Clears all stage entries and related cached data so a new import batch can start cleanly.
     */
    public void resetAll() {
        logger.info("Resetting staging area. Clearing {} staged entries", masterEntries.size());
        cancelStaging();
        masterEntries.clear();
        deleteThumbnailCache();
        createSessionDirectory();
    }

    /**
     * Removes the specified staged entry from the import list and clears its selection listener.
     *
     * @param stageEntry the staged entry to remove
     */
    public void removeStageEntry(StageEntry stageEntry) {
        stageEntry.getIsSelectedProperty().set(false);
        // stageEntry.getIsSelectedProperty().removeListener(selectionListener);

        if (stageEntry.companion != null && autoPairProperty.get()) {
            stageEntry.companion.getIsSelectedProperty().set(false);
            // stageEntry.companion.getIsSelectedProperty().removeListener(selectionListener);
            masterEntries.remove(stageEntry.companion);
        }

        masterEntries.remove(stageEntry);
    }

    public IntegerProperty getSelectedFilesProperty() {
        return selectedFilesProperty;
    }

    public ReadOnlyIntegerProperty getStagedFilesProperty() {
        return stagedFilesProperty;
    }

    public BooleanProperty getHideDuplicatesProperty() {
        return hideDuplicatesProperty;
    }

    public StringProperty getNameProperty() {
        return nameProperty;
    }

    public BooleanProperty getAutoPairProperty() {
        return autoPairProperty;
    }

    public ReadOnlyLongProperty getStageSizeInBytes() {
        return stageSizeInBytes;
    }

    /**
     * Removes all selected stage entries from the import list.
     */
    public void removeSelection() {
        List<StageEntry> toRemove = filteredEntries.stream()
                .filter(entry -> entry.getIsSelectedProperty().get())
                .toList();
        toRemove.forEach(this::removeStageEntry);
    }

    public void toggleAutoPair() {
        autoPairProperty.set(!autoPairProperty.get());
    }

    public void toggleHideDuplicates() {
        hideDuplicatesProperty.set(!hideDuplicatesProperty.get());
    }

    public ReadOnlyBooleanProperty getStagingProperty() {
        return stagingProperty;
    }

    @Override
    public void close() throws Exception {
        cancelStaging();
        deleteThumbnailCache();
    }
}
