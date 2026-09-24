package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.app.managers.ApplicationManager;
import hu.elte.ik.thesis.cinegrade.app.managers.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.infra.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.SplashScreenController;
import hu.elte.ik.thesis.cinegrade.tasks.AppInitService;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App extends Application {

    private ApplicationManager applicationManager;
    private ThreadPoolManager threadPoolManager;
    private RequirementChecker requirementChecker;
    private TaskManager taskManager;
    private Logger logger;

    private Stage splashStage;

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
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        splashStage = new Stage(StageStyle.TRANSPARENT);
        logger.debug("Application start method started");
        primaryStage.setTitle("CineGrade");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/splashScreen.fxml"));
        Parent root = loader.load();
        SplashScreenController controller = loader.getController();

        AppInitService appInitService = new AppInitService(requirementChecker);
        taskManager.configure(appInitService);

        controller.getTaskNameLabel().textProperty().bind(appInitService.messageProperty());
        appInitService.progressProperty().addListener((obs, oldVal, newVal) -> {
            controller.setProgress(newVal.doubleValue());
        });

        appInitService.setOnSucceeded(e -> {
            logger.info("Startup sequence completed successfully");
            PauseTransition pause = new PauseTransition(Duration.millis(400));
            pause.setOnFinished(ev -> loadMainMenu(primaryStage));
            pause.play();
        });

        appInitService.setOnFailed(e -> {
            Throwable ex = appInitService.getException();
            logger.error("Startup failed with exception: ", ex);
        });

        Scene scene = new Scene(root);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        scene.setFill(Color.TRANSPARENT);
        splashStage.show();
        splashStage.toFront();

        logger.debug("Task setup finished, starting AppInitService");
        appInitService.start();
    }

    private void loadMainMenu(Stage primaryStage) {

        // UI Controls
        splashStage.hide();

        Label label = new Label("CineGrade Diagnostic & Test Bench");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -clr-text-primary;");

        Button themeToggleBtn = new Button("Toggle Light / Dark Theme");
        themeToggleBtn.getStyleClass().addAll("btn", "btn-secondary");

        Button uncaughtExceptionBtn = new Button("Throw Uncaught Exception (Generic)");
        uncaughtExceptionBtn.getStyleClass().addAll("btn", "btn-secondary");

        Button uncaughtExceptionBtn2 = new Button("Throw Uncaught CineGradeException");
        uncaughtExceptionBtn2.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException = new Button("Trigger Toast Warning (Delete Failed)");
        cinegradeException.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException2 = new Button("Trigger Toast Error (Write Failed)");
        cinegradeException2.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException3 = new Button("Trigger Fatal Error Modal (DB Schema Failed)");
        cinegradeException3.getStyleClass().addAll("btn", "btn-danger");

        // Center Content Layout
        VBox contentBox = new VBox(12, label, themeToggleBtn, uncaughtExceptionBtn, uncaughtExceptionBtn2,
                cinegradeException, cinegradeException2, cinegradeException3);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(24));

        // Toast Container anchored to top-right
        VBox toastBox = new VBox(10);
        toastBox.setAlignment(Pos.TOP_RIGHT);
        toastBox.setPickOnBounds(false);

        // Root AnchorPane
        AnchorPane rootPane = new AnchorPane();
        rootPane.setStyle("-fx-background-color: -clr-almostdarkgrey;");
        rootPane.getStyleClass().add("theme-dark");

        AnchorPane.setTopAnchor(contentBox, 0.0);
        AnchorPane.setBottomAnchor(contentBox, 0.0);
        AnchorPane.setLeftAnchor(contentBox, 0.0);
        AnchorPane.setRightAnchor(contentBox, 0.0);

        AnchorPane.setTopAnchor(toastBox, 20.0);
        AnchorPane.setRightAnchor(toastBox, 20.0);

        rootPane.getChildren().addAll(contentBox, toastBox);

        ErrorHandler.getInstance().setToastContainer(toastBox);

        Scene scene = new Scene(rootPane, 720, 520);
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        themeToggleBtn.setOnAction(e -> {
            ObservableList<String> classes = rootPane.getStyleClass();
            if (classes.contains("theme-light")) {
                classes.remove("theme-light");
                classes.add("theme-dark");
            } else {
                classes.remove("theme-dark");
                classes.add("theme-light");
            }
        });

        uncaughtExceptionBtn.setOnMouseClicked(e -> {
            throw new RuntimeException("Generic unexpected runtime exception");
        });

        uncaughtExceptionBtn2.setOnMouseClicked(e -> {
            throw new CineGradeException(ErrorCode.DB_SCHEMA_INIT_FAILED);
        });

        cinegradeException.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.FILE_DELETE_FAILED));
        });

        cinegradeException2.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.FILE_WRITE_FAILED));
        });

        cinegradeException3.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.DB_SCHEMA_INIT_FAILED));
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
