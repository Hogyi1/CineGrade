package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.FileProbeResult;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ImportProbeService extends Service<List<FileProbeResult>> {

    private static final Logger logger = LogManager.getLogger(ImportProbeService.class);
    private final ExifToolService exifToolService;
    private final List<File> droppedFiles;

    public ImportProbeService(ExifToolService exifToolService, List<File> droppedFiles) {
        this.exifToolService = exifToolService;
        this.droppedFiles = droppedFiles;
    }

    @Override
    protected Task<List<FileProbeResult>> createTask() {

        if (droppedFiles == null || droppedFiles.isEmpty()) {
            return new Task<>() {
                @Override
                protected List<FileProbeResult> call() {
                    return List.of();
                }
            };
        }

        // If a directory is dropped, leave sublists alone
        List<File> fileList = expand(droppedFiles);

        return new Task<>() {

            @Override
            protected List<FileProbeResult> call() throws Exception {
                // 0. Filter valid, existing candidate files
                List<Path> paths = fileList.stream()
                        .filter(Objects::nonNull)
                        .map(File::toPath)
                        .filter(Files::exists)
                        .filter(Files::isRegularFile)
                        .distinct()
                        .toList();

                if (paths.isEmpty()) {
                    logger.info("No valid candidate files to stage");
                    return List.of();
                }

                logger.debug("Candidate files expanded from {} dropped item(s) to {} valid regular file(s)", droppedFiles.size(), paths.size());

                // 1. Probe candidate files with ExifTool
                List<FileProbeResult> probeResults = exifToolService.probeFiles(paths, this::isCancelled);
                logger.debug("ExifTool probe finished with {} result(s)", probeResults.size());

                return probeResults;
            }
        };
    }

    private List<File> expand(List<File> droppedFiles) {
        List<File> candidates = new ArrayList<>();
        droppedFiles.stream()
                .filter(Objects::nonNull)
                .flatMap(file -> {
                    if (file.isDirectory()) {
                        try (Stream<Path> walk = Files.walk(file.toPath())) {
                            return walk.filter(Files::isRegularFile).toList().stream();
                        } catch (IOException ex) {
                            logger.error("Error occurred while walking directory: {}", file, ex);
                            return Stream.empty();
                        }
                    }
                    return Stream.of(file.toPath());
                })
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .forEach(candidates::add);
        return candidates;
    }
}
