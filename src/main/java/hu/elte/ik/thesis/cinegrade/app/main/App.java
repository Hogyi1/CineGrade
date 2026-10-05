package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.app.managers.ApplicationManager;
import hu.elte.ik.thesis.cinegrade.app.managers.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.app.managers.catalog.CatalogManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ExportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.ControllerFactory;
import hu.elte.ik.thesis.cinegrade.domain.navigation.SceneManager;
import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.javafx.ui.catalog.ProjectMenuController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.SplashScreenController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.editing.*;
import hu.elte.ik.thesis.cinegrade.javafx.ui.test.ErrorTestController;
import hu.elte.ik.thesis.cinegrade.domain.tasks.AppInitService;
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
    private SceneManager sceneManager;
    private TaskManager taskManager;
    private ControllerFactory controllerFactory;
    private RequirementChecker requirementChecker;
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

            SplashScreenController splashScreenController = sceneManager.showSplash();
            splashScreenController.startService();

            logger.debug("Task setup finished, starting AppInitService");
        } catch (Exception ex) {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.UNKNOWN_ERROR, ex, "FATAL error inside application startup method"));
        }
    }

    /**
     * Creates the application initialization service
     * @return the initialized AppInitService
     */
    private AppInitService createAppInitService(){
        AppInitService appInitService = new AppInitService(requirementChecker);
        taskManager.configure(appInitService);

        appInitService.setOnSucceeded(e -> {
            logger.info("Startup sequence completed successfully");

            createDatabaseManagers();
            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            pause.setOnFinished(ev -> sceneManager.switchView(ViewType.PROJECT_MENU));
            pause.play();
        });

        appInitService.setOnFailed(e -> {
            Throwable ex = appInitService.getException();
            ErrorHandler.getInstance().handle(ex);
        });

        return appInitService;
    }

    /**
     * Register all view controllers
     */
    private void registerControllers() {
        logger.info("Registering view controllers");
        controllerFactory.register(ProjectMenuController.class, () -> new ProjectMenuController(catalogManager, userManager, sceneManager));
        controllerFactory.register(ErrorTestController.class, () -> new ErrorTestController(sceneManager));
        controllerFactory.register(SplashScreenController.class, () -> new SplashScreenController(createAppInitService()));
        controllerFactory.register(WorkspaceController.class, () -> new WorkspaceController(catalogManager.getCurrentCatalogContext(), controllerFactory));
        controllerFactory.register(NavigationController.class, () -> new NavigationController(sceneManager, userManager));
        controllerFactory.register(ImportPanelController.class, ImportPanelController::new);
        controllerFactory.register(DevelopmentPanelController.class, DevelopmentPanelController::new);
        controllerFactory.register(ExportPanelController.class, ExportPanelController::new);
    }

    /**
     * Create bootstrap managers
     */
    private void createBootstrapManagers(Stage primaryStage) {
        logger.info("Creating bootstrap managers");
        controllerFactory = new ControllerFactory();
        sceneManager = new SceneManager(primaryStage, controllerFactory);
        applicationManager = new ApplicationManager();
        threadPoolManager = new ThreadPoolManager();
        taskManager = new TaskManager(threadPoolManager);
        requirementChecker = new RequirementChecker();
    }

    /**
     * Create database-dependent managers
     */
    private void createDatabaseManagers() {
        logger.info("Creating database-dependent managers");
        catalogManager = new CatalogManager(taskManager);
        userManager = new UserManager();
    }
}
