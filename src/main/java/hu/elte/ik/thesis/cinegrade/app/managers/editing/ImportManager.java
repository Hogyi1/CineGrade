package hu.elte.ik.thesis.cinegrade.app.managers.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportBatchService;
import hu.elte.ik.thesis.cinegrade.domain.tasks.ImportStageService;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibRaw;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibrawService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ImportManager {

    private final ObservableList<StageEntry> stageEntries = FXCollections.observableArrayList();
    private final IntegerProperty stageSize = new SimpleIntegerProperty(0);
    private final Catalog catalog;
    private final ImportBatchService importBatchService;
    private final ImportStageService importStageService;
    private final TaskManager taskManager;
    private final ExifToolService exifToolService;

    public ImportManager(Connection connection, Catalog catalog, TaskManager taskManager) {
        importBatchService = new ImportBatchService();
        importStageService = new ImportStageService();
        exifToolService = new ExifToolService();
        this.catalog = catalog;
        this.taskManager = taskManager;
    }

    public void stagePhotos(List<File> files) {
        List<Path> paths = files.stream().map(File::toPath).toList();
        List<FileProbeResult> results = exifToolService.probeFiles(paths, null);
        List<StageEntry> entries = pairDuplicates(results);
        entries.forEach(this::attachThumbnail);
        stageEntries.addAll(entries);
        calculateStageSize();
    }

    public void startImportBatchService() {

    }

    private List<StageEntry> pairDuplicates(List<FileProbeResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .filter(FileProbeResult::isImage)
                .collect(Collectors.groupingBy(r -> getPathWithoutExtension(r.toPath())))
                .values()
                .stream()
                .map(this::createStageEntry)
                .toList();
    }

    private StageEntry createStageEntry(List<FileProbeResult> group) {
        FileProbeResult raw = group.stream().filter(FileProbeResult::isRaw).findFirst().orElse(null);
        FileProbeResult nonRaw = group.stream().filter(r -> !r.isRaw()).findFirst().orElse(null);

        StageEntry entry = new StageEntry();

        if (raw != null) {
            File rawFile = raw.toPath().toFile();
            entry.setFile(rawFile);
            entry.setRaw(true);
            entry.setSize(rawFile.length());
            entry.setFileType(raw.fileType());

            if (nonRaw != null) {
                Path jpegPath = nonRaw.toPath();
                entry.setJpegPath(jpegPath);
                entry.setThumbnailPath(jpegPath);
            } else {
                entry.setJpegPath(null);
                attachThumbnail(entry);
            }
        } else if (nonRaw != null) {
            File imageFile = nonRaw.toPath().toFile();
            Path imagePath = nonRaw.toPath();

            entry.setFile(imageFile);
            entry.setRaw(false);
            entry.setSize(imageFile.length());
            entry.setFileType(nonRaw.fileType());
            entry.setJpegPath(imagePath);
            entry.setThumbnailPath(imagePath);
        }

        return entry;
    }

    private String getPathWithoutExtension(Path key) {
        String fullPath = key.toAbsolutePath().normalize().toString();
        int dotIndex = fullPath.lastIndexOf('.');
        int separatorIndex = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));

        if (dotIndex > separatorIndex) {
            return fullPath.substring(0, dotIndex).toLowerCase(Locale.ROOT);
        }
        return fullPath.toLowerCase(Locale.ROOT);
    }

    private void attachThumbnail(StageEntry entry) {
        LibrawService librawService = new LibrawService();
        String thumbnailName = entry.getFile().getName() + "_thumb.jpg";
        boolean isSuccess = librawService.extractThumbnail(entry.getFile(), catalog.getTempDirectory().resolve(thumbnailName));
        if (isSuccess) {
            entry.setThumbnailPath(catalog.getTempDirectory().resolve(thumbnailName));
        } else {
            entry.setThumbnailPath(null);
        }
    }

    private void deleteThumbnailCache(StageEntry entry) {

    }

    private void calculateStageSize() {

    }

    public void removeEntry(StageEntry entry) {
        stageEntries.remove(entry);
        deleteThumbnailCache(entry);
    }

    private void validateFile(File file) {

    }
}
