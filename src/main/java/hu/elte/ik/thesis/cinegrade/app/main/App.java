package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.app.managers.ApplicationManager;
import hu.elte.ik.thesis.cinegrade.app.managers.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.app.managers.catalog.CatalogManager;
import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.ControllerFactory;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.javafx.ui.catalog.MainMenuController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.SplashScreenController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.test.ErrorTestController;
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
    private CatalogManager catalogManager;
    private UserManager userManager;
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
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.debug("Application start method invoked");

            createBootstrapManagers(primaryStage);
            registerControllers();

            var appInitService = createAppInitService();
            SplashScreenController controller = navigationManager.showSplash();
            controller.getTaskNameLabel().textProperty().bind(appInitService.messageProperty());
            appInitService.progressProperty().addListener((obs, oldVal, newVal) -> {
                controller.setProgress(newVal.doubleValue());
            });

            logger.debug("Task setup finished, starting AppInitService");
            appInitService.start();
        } catch (Exception ex) {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.UNKNOWN_ERROR, ex, "FATAL error inside application startup method"));
        }
    }

    private AppInitService createAppInitService(){
        AppInitService appInitService = new AppInitService(requirementChecker);
        taskManager.configure(appInitService);

        appInitService.setOnSucceeded(e -> {
            logger.info("Startup sequence completed successfully");

            createDatabaseManagers();
            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            pause.setOnFinished(ev -> navigationManager.switchView(ViewType.TEST_MENU));
            pause.play();
        });

        appInitService.setOnFailed(e -> {
            Throwable ex = appInitService.getException();
            logger.error("Startup failed with exception: ", ex);
        });

        return appInitService;
    }

    private void registerControllers() {
        logger.info("Registering view controllers");
        controllerFactory.register(MainMenuController.class, () -> new MainMenuController(catalogManager, userManager, navigationManager));
        controllerFactory.register(ErrorTestController.class, () -> new ErrorTestController(navigationManager));
    }

    private void createBootstrapManagers(Stage primaryStage) {
        logger.info("Creating bootstrap managers");
        controllerFactory = new ControllerFactory();
        navigationManager = new NavigationManager(primaryStage, controllerFactory);
        applicationManager = new ApplicationManager();
        threadPoolManager = new ThreadPoolManager();
        requirementChecker = new RequirementChecker();
        taskManager = new TaskManager(threadPoolManager);
    }

    private void createDatabaseManagers() {
        logger.info("Creating database-dependent managers");
        catalogManager = new CatalogManager();
        userManager = new UserManager();
    }
}
