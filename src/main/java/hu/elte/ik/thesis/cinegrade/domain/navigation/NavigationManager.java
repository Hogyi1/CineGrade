package hu.elte.ik.thesis.cinegrade.domain.navigation;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.text.View;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NavigationManager {

    private static final Logger logger = LogManager.getLogger(NavigationManager.class);
    private final ConcurrentLinkedQueue<ViewHistory> viewHistories = new ConcurrentLinkedQueue<>();
    private final Callback<Class<?>, Object> controllerFactory;
    private Stage primaryStage, splashStage;
    private Scene primaryScene;
    private ViewType currentView;
    private StackPane contentLayer, rootShell;
    private VBox toastContainer;

    public NavigationManager(Stage primaryStage, ViewType currentView, Callback<Class<?>, Object> controllerFactory) {
        this.primaryStage = primaryStage;
        this.currentView = currentView;
        this.controllerFactory = controllerFactory;

        primaryScene = primaryStage.getScene();
        initShell();
        switchView(currentView);
    }

    public NavigationManager(Stage primaryStage, Callback<Class<?>, Object> controllerFactory) {
        this.primaryStage = primaryStage;
        this.primaryScene = primaryStage.getScene();
        this.controllerFactory = controllerFactory;

        this.currentView = null;
        initShell();
    }

    public <T> T switchView(ViewType nextView, BiConsumer<Scene, Stage> sceneHandling) {
        ViewHistory previousHistory = viewHistories.peek();
        viewHistories.offer(new ViewHistory(previousHistory, currentView, nextView));

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
            contentLayer.getChildren().setAll(viewRoot);

            if (!primaryStage.isShowing()) {
                primaryStage.centerOnScreen();
                primaryStage.toFront();
                splashStage.close();
                primaryStage.show();
            }

            return loader.getController();
        } catch (IOException ioex) {
            throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, ioex);
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
        rootShell.getStyleClass().add("theme-dark");

        ErrorHandler.getInstance().setToastContainer(toastContainer);

        primaryScene = new Scene(rootShell);
        primaryScene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        primaryStage.setScene(primaryScene);
    }

    public Stage getActiveStage() {
        return primaryStage;
    }

    private record ViewHistory(ViewHistory previous, ViewType current, ViewType next) {
    }
}
