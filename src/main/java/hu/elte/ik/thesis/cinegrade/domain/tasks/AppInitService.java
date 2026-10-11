package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.results.AppInitResult;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.storage.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.infra.storage.database.StorageService;
import javafx.animation.PauseTransition;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppInitService extends Service<AppInitResult> {

    private final RequirementChecker requirementChecker;
    private final StorageService storageService;

    public AppInitService(RequirementChecker requirementChecker) {
        this.requirementChecker = requirementChecker;
        this.storageService = new StorageService();
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
                PauseTransition pause = new PauseTransition(Duration.millis(1000));
                pause.play();

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
                ThemeManager.getInstance().initDatabase(AppDatabase.getInstance().getConnection());
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
        Path projectDir = Path.of(AppConfig.INSTANCE.getProjectDirectory());

        try {
            storageService.ensureDirectory(rootDir);
            storageService.ensureDirectory(logDir);
            storageService.ensureDirectory(presetDir);
            storageService.ensureDirectory(tempDir);
            storageService.ensureDirectory(projectDir);
        } catch (CineGradeException e) {
            throw new CineGradeException(ErrorCode.REQ_APP_DATA_DIR_NOT_WRITABLE, e, rootDir.toAbsolutePath().toString());
        }

        cleanTempDirectory(tempDir);
    }

    private void cleanTempDirectory(Path tempDir) {

    }



    public ReadOnlyStringProperty taskProperty() {
        return messageProperty();
    }
}
