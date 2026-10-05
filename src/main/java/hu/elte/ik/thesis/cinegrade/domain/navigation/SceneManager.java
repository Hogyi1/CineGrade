package hu.elte.ik.thesis.cinegrade.domain.navigation;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorModalController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public class SceneManager {

    private static final Logger logger = LogManager.getLogger(SceneManager.class);
    private final Callback<Class<?>, Object> controllerFactory;
    private Stage primaryStage, splashStage;
    private Scene primaryScene;
    private ViewType currentView;
    private StackPane contentLayer, rootShell;
    private VBox toastContainer;

    public SceneManager(Stage primaryStage, ViewType currentView, Callback<Class<?>, Object> controllerFactory) {
        this.primaryStage = primaryStage;
        this.currentView = currentView;
        this.controllerFactory = controllerFactory;

        primaryScene = primaryStage.getScene();
        initShell();
        switchView(currentView);
    }

    public SceneManager(Stage primaryStage, Callback<Class<?>, Object> controllerFactory) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryStage.getScene();
        this.controllerFactory = controllerFactory;

        this.currentView = null;
        initShell();
    }

    public <T> T switchView(ViewType nextView, BiConsumer<Scene, Stage> sceneHandling) {
        if (nextView == null) {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "View cannot be null");
        }
        //Do scene change
        if (sceneHandling != null) {
            // tofront tocenter stagemodality etc
            sceneHandling.accept(primaryScene, primaryStage);
        }

        String view = currentView != null ? currentView.getTitle() : "Splash";
        logger.info("Switched scenes from {} to {}", view, nextView.getTitle());

        try {
            FXMLLoader loader = createLoader(nextView.getFxmlPath());
            Parent viewRoot = loader.load();

            primaryStage.setTitle(nextView.getTitle());
            primaryStage.setMaximized(nextView.isFullScreen());
            contentLayer.getChildren().setAll(viewRoot);

            if (!primaryStage.isShowing()) {
                primaryStage.centerOnScreen();
                primaryStage.toFront();
                splashStage.close();
                primaryStage.show();
            }

            currentView = nextView;
            return loader.getController();
        } catch (IOException ioex) {
            throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, ioex, nextView.getFxmlPath());
        }
    }

    public <T> T switchView(ViewType nextView) {
        return switchView(nextView, null);
    }

    public Window getActiveWindow() {
        return Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
    }

    private FXMLLoader createLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(controllerFactory);
        return loader;
    }

    public <T> T showSplash() {
        splashStage = new Stage(StageStyle.TRANSPARENT);

        FXMLLoader loader = createLoader(ViewType.SPLASH.getFxmlPath());
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.setupStylesheet(scene);
            scene.setFill(Color.TRANSPARENT);

            primaryStage.hide();
            splashStage.setScene(scene);
            splashStage.centerOnScreen();
            splashStage.show();
            splashStage.toFront();

            return loader.getController();
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, ex, "Failed to display splash screen");
        }
    }

    private void initShell() {
        contentLayer = new StackPane();

        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPickOnBounds(false);
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(toastContainer, new Insets(20));

        rootShell = new StackPane(contentLayer, toastContainer);
        rootShell.getStyleClass().add(ThemeManager.get().getStyleClass());

        ErrorHandler.getInstance().setToastContainer(toastContainer);
        ErrorHandler.getInstance().setSceneManager(this);

        primaryScene = new Scene(rootShell);
        ThemeManager.setupStylesheet(primaryScene);
        primaryStage.setScene(primaryScene);

        rootShell.getStyleClass().add(ThemeManager.get().getStyleClass());
        ThemeManager.getInstance().themeProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue != null) rootShell.getStyleClass().remove(oldValue.getStyleClass());
            if (newValue != null) rootShell.getStyleClass().add(newValue.getStyleClass());
        });
    }

    public void showFatalErrorModal(CineGradeException exception) {
        Platform.runLater(() -> {
            try {
                Platform.setImplicitExit(false);

                FXMLLoader loader = createLoader("/fxmls/errorModal.fxml");
                Parent root = loader.load();
                ErrorModalController controller = loader.getController();
                controller.setupDialog(exception);

                root.getStyleClass().add(ThemeManager.get().getStyleClass());

                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.setTitle("CineGrade — Fatal Error");
                modalStage.setResizable(false);
                modalStage.setScene(new Scene(root));
                modalStage.centerOnScreen();

                // Close all existing open windows so the main app disappears completely
                for (Window window : Window.getWindows().toArray(new Window[0])) {
                    if (window instanceof Stage stage && stage != modalStage) {
                        stage.close();
                    }
                }

                modalStage.showAndWait();
                Platform.exit();
            } catch (Exception e) {
                logger.error("Failed to display fatal error modal", e);
                Platform.exit();
            }
        });
    }
}
