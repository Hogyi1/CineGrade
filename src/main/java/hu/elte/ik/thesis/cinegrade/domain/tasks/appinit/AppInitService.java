package hu.elte.ik.thesis.cinegrade.domain.tasks.appinit;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
import javafx.animation.PauseTransition;
import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class AppInitService extends Service<AppInitResult> {

    private final RequirementChecker requirementChecker;
    private final StorageService storageService;
    private final AtomicInteger stepCounter = new AtomicInteger(0);
    private final ThemeManager themeManager;

    public AppInitService(RequirementChecker requirementChecker, ThemeManager themeManager, StorageService storageService) {
        this.requirementChecker = requirementChecker;
        this.themeManager = themeManager;
        this.storageService = storageService;
    }

    @Override
    protected Task<AppInitResult> createTask() throws CineGradeException {

        return new Task<AppInitResult>() {
            @Override
            protected AppInitResult call() throws Exception {
                int totalSteps = 10;
                stepCounter.set(0);

                updateMessage("Checking Java version...");
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                boolean success = requirementChecker.checkJavaVersion();
                if (!success) {
                    throw new CineGradeException(ErrorCode.REQ_JAVA_VERSION_INCOMPATIBLE);
                }
                updateProgress(stepCounter.getAndIncrement(), totalSteps);
                PauseTransition pause = new PauseTransition(Duration.millis(1000));
                pause.play();

                updateMessage("Checking native binaries...");
                requirementChecker.checkNativeBinaries();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Verifying core UI resources...");
                requirementChecker.checkCriticalResources();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Checking ExifTool...");
                requirementChecker.checkExiftool();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Checking FFmpeg...");
                requirementChecker.checkFFmpeg();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Checking available space...");
                storageService.sufficientSpace(Path.of(AppConfig.INSTANCE.getRootDirectory()), AppConfig.INSTANCE.getRequiredSpace());
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Preparing workspace directories and cleaning temp files...");
                setupStorageAndCleanTemp();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Setting up app database connection...");
                AppDatabase.getInstance().openConnection(Path.of(AppConfig.INSTANCE.getRootDirectory(), "app.db"));
                themeManager.initDatabase(AppDatabase.getInstance().getConnection());
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Running benchmarks...");
                requirementChecker.runPerformanceBenchmark();
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

                updateMessage("Engine warmup...");
                // TODO add engine warmup method
                updateProgress(stepCounter.getAndIncrement(), totalSteps);

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
    }

    public ReadOnlyStringProperty taskProperty() {
        return messageProperty();
    }
}
