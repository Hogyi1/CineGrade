package hu.elte.ik.thesis.cinegrade.tasks;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.results.AppInitResult;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorHandler;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppInitService extends Service<AppInitResult> {

    private final RequirementChecker requirementChecker;

    public AppInitService(RequirementChecker requirementChecker) {
        this.requirementChecker = requirementChecker;
    }

    @Override
    protected Task<AppInitResult> createTask() throws CineGradeException {

        return new Task<AppInitResult>() {
            @Override
            protected AppInitResult call() throws Exception {
                int totalSteps = 8;

                updateMessage("Checking Java version...");
                updateProgress(0, totalSteps);

                boolean success = requirementChecker.checkJavaVersion();
                if (!success) {
                    throw new CineGradeException(ErrorCode.REQ_JAVA_VERSION_INCOMPATIBLE);
                }
                updateProgress(1, totalSteps);
                //delay(500);

                updateMessage("Checking native binaries...");
                requirementChecker.checkNativeBinaries();
                updateProgress(2, totalSteps);

                updateMessage("Verifying core UI resources...");
                requirementChecker.checkCriticalResources();
                updateProgress(3, totalSteps);

                updateMessage("Checking ExifTool...");
                requirementChecker.checkExiftool();
                updateProgress(4, totalSteps);

                updateMessage("Checking FFmpeg...");
                requirementChecker.checkFFmpeg();
                updateProgress(5, totalSteps);

                updateMessage("Preparing workspace directories and cleaning temp files...");
                setupStorageAndCleanTemp();
                updateProgress(6, totalSteps);

                updateMessage("Setting up app database connection...");
                AppDatabase.getInstance().openConnection(Path.of(AppConfig.INSTANCE.getRootDirectory(), "app.db"));
                updateProgress(7, totalSteps);

                updateMessage("Engine warmup...");
                // TODO add engine warmup method
                updateProgress(8, totalSteps);

                updateMessage("Ready!");
                return AppInitResult.success("Application started successfully");
            }
        };
    }

    /**
     * Ensures all required application sandbox directories exist (~/.cinegrade/logs, presets, temp)
     * and deletes stale/orphaned files left behind from previous sessions in the temp directory.
     */
    public void setupStorageAndCleanTemp() {

        Path rootDir = Path.of(AppConfig.INSTANCE.getRootDirectory());
        Path logDir = Path.of(AppConfig.INSTANCE.getLogDirectory());
        Path presetDir = Path.of(AppConfig.INSTANCE.getPresetDirectory());
        Path tempDir = rootDir.resolve("temp");

        try {
            Files.createDirectories(rootDir);
            Files.createDirectories(logDir);
            Files.createDirectories(presetDir);
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new CineGradeException(ErrorCode.REQ_APP_DATA_DIR_NOT_WRITABLE, e, rootDir.toAbsolutePath().toString());
        }

        cleanTempDirectory(tempDir);
    }

    private void cleanTempDirectory(Path tempDir) {
        if (!Files.exists(tempDir) || !Files.isDirectory(tempDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
            for (Path entry : stream) {
                deleteRecursively(entry);
            }
        } catch (IOException e) {
            LogManager.getLogger(AppInitService.class).warn("Failed to read temp directory during cleanup: {}", tempDir, e);
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                    for (Path child : entries) {
                        deleteRecursively(child);
                    }
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LogManager.getLogger(AppInitService.class).warn("Could not delete stale temp file (might be locked): {}", path, e);
        }
    }

    public ReadOnlyStringProperty taskProperty() {
        return messageProperty();
    }

    private void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
