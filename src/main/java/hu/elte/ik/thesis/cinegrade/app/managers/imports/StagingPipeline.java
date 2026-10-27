package hu.elte.ik.thesis.cinegrade.app.managers.imports;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageResult;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportProbeService;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportStageService;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.utils.thumbnail.ThumbnailFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Turns dropped files into staged entries: probe (ExifTool, io pool) → batches of {@link #BATCH_COUNT}
 * → one {@link ImportStageService} per batch (task pool, thumbnails fan out to the cpu pool).
 * <p>
 * Dropping more files while staging runs appends them: their probe runs in parallel, its batches are
 * queued behind the current ones, and batches always run one at a time, so each batch's duplicate
 * check sees everything staged before it.
 * <p>
 * The services are deliberately not registered with the TaskManager (no toasts, no UI cancel button);
 * only {@link #cancel()} stops them. All methods must be called on the FX thread.
 */
public class StagingPipeline {

    private static final Logger logger = LogManager.getLogger(StagingPipeline.class);
    private static final int BATCH_COUNT = 25;

    private final StagingArea stagingArea;
    private final ThumbnailFactory thumbnailFactory;
    private final ExifToolService exifToolService;
    private final ExecutorService cpuPool;
    private final ExecutorService ioPool;
    private final ExecutorService taskPool;
    private final Supplier<List<PhotoEntry>> importedPhotos;

    private final ArrayDeque<List<FileProbeResult>> pendingBatches = new ArrayDeque<>();
    private final List<Service<?>> active = new ArrayList<>();

    private final BooleanProperty runningProperty = new SimpleBooleanProperty(false);

    // Bumped by cancel(): callbacks of an older run see a different number and drop their results
    private int generation;
    private int probesInFlight;
    private boolean batchActive;

    public StagingPipeline(StagingArea stagingArea,
                           ThumbnailFactory thumbnailFactory,
                           ExifToolService exifToolService,
                           ExecutorService cpuPool,
                           ExecutorService ioPool,
                           ExecutorService taskPool,
                           Supplier<List<PhotoEntry>> importedPhotos) {
        this.stagingArea = stagingArea;
        this.thumbnailFactory = thumbnailFactory;
        this.exifToolService = exifToolService;
        this.cpuPool = cpuPool;
        this.ioPool = ioPool;
        this.taskPool = taskPool;
        this.importedPhotos = importedPhotos;

        stagingArea.getStagingProperty().bind(runningProperty);
    }

    /**
     * Probes and stages the files. If staging is already running, the files are appended to it.
     *
     * @param files the dropped or chosen files and directories
     */
    public void stage(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        int gen = generation;
        ImportProbeService probe = new ImportProbeService(exifToolService, files);
        probe.setExecutor(ioPool);

        probe.setOnSucceeded(e -> {
            active.remove(probe);
            if (gen != generation) return;
            probesInFlight--;

            List<FileProbeResult> results = probe.getValue();
            enqueue(results);
            logger.debug("Probe finished with {} result(s), {} batch(es) pending",
                    results == null ? 0 : results.size(), pendingBatches.size());

            // A running batch picks the new ones up when it finishes
            if (!batchActive) {
                stageNextBatch(gen);
            }
            updateRunning();
        });

        probe.setOnFailed(e -> {
            active.remove(probe);
            if (gen != generation) return;
            probesInFlight--;
            ErrorHandler.getInstance().handle(probe.getException());
            updateRunning();
        });

        // cancel() already reset the counters for its own generation
        probe.setOnCancelled(e -> active.remove(probe));

        active.add(probe);
        probesInFlight++;
        updateRunning();
        probe.start();
    }

    /**
     * Stops every probe and batch, drops queued batches, and makes late results of this run be ignored.
     * Entries already added to the staging area stay there.
     */
    public void cancel() {
        generation++;
        pendingBatches.clear();
        new ArrayList<>(active).forEach(Service::cancel);
        active.clear();
        probesInFlight = 0;
        batchActive = false;
        updateRunning();
    }

    private void stageNextBatch(int gen) {
        if (gen != generation) return;

        List<FileProbeResult> batch = pendingBatches.poll();
        batchActive = batch != null;
        updateRunning();
        if (batch == null) {
            return;
        }

        // Snapshot the inputs on the FX thread; the service only reads these copies
        ImportStageService service = new ImportStageService(thumbnailFactory, cpuPool, batch,
                List.copyOf(stagingArea.getReadOnlyEntries()), importedPhotos);
        service.setExecutor(taskPool);

        service.setOnSucceeded(e -> {
            active.remove(service);
            if (gen != generation) return;

            StageResult result = service.getValue();
            result.fxUpdates().forEach(Runnable::run);
            stagingArea.addAll(result.entries());

            logger.info("Added {} entries. Total staged: {}", result.entries().size(), stagingArea.getMasterEntries().size());
            stageNextBatch(gen);
        });

        service.setOnFailed(e -> {
            active.remove(service);
            if (gen != generation) return;
            logger.error("Staging batch of {} file(s) failed, skipping it", batch.size(), service.getException());
            stageNextBatch(gen);
        });

        service.setOnCancelled(e -> active.remove(service));

        active.add(service);
        service.start();
    }

    private void updateRunning() {
        runningProperty.set(probesInFlight > 0 || batchActive);
    }

    private void enqueue(List<FileProbeResult> results) {
        if (results == null) return;
        for (int i = 0; i < results.size(); i += BATCH_COUNT) {
            pendingBatches.add(results.subList(i, Math.min(i + BATCH_COUNT, results.size())));
        }
    }
}
