package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.app.managers.ApplicationManager;
import hu.elte.ik.thesis.cinegrade.app.managers.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.ControllerFactory;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.SplashScreenController;
import hu.elte.ik.thesis.cinegrade.tasks.AppInitService;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App extends Application {

    private ApplicationManager applicationManager;
    private ThreadPoolManager threadPoolManager;
    private RequirementChecker requirementChecker;
    private NavigationManager navigationManager;
    private TaskManager taskManager;
    private ControllerFactory controllerFactory;
    private Logger logger;

    @Override
    public void init() {
        LoggerUtils.init();
        logger = LogManager.getLogger(App.class);
        logger.info("Application starting...");

        logger.debug("Installing global error handler");
        ErrorHandler.getInstance().install();

        logger.debug("Creating managers");
        applicationManager = new ApplicationManager();
        threadPoolManager = new ThreadPoolManager();
        requirementChecker = new RequirementChecker();
        taskManager = new TaskManager(threadPoolManager);
        controllerFactory = new ControllerFactory();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        navigationManager = new NavigationManager(primaryStage, controllerFactory);

        logger.debug("Application start method started");
        AppInitService appInitService = new AppInitService(requirementChecker);
        taskManager.configure(appInitService);

        appInitService.setOnSucceeded(e -> {
            logger.info("Startup sequence completed successfully");
            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            pause.setOnFinished(ev -> navigationManager.switchView(ViewType.TEST_MENU));
            pause.play();
        });

        appInitService.setOnFailed(e -> {
            Throwable ex = appInitService.getException();
            logger.error("Startup failed with exception: ", ex);
        });

        SplashScreenController controller = navigationManager.showSplash();
        controller.getTaskNameLabel().textProperty().bind(appInitService.messageProperty());
        appInitService.progressProperty().addListener((obs, oldVal, newVal) -> {
            controller.setProgress(newVal.doubleValue());
        });

        logger.debug("Task setup finished, starting AppInitService");
        appInitService.start();
    }
}
