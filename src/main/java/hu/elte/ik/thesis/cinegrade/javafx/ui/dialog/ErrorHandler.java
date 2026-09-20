package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.enums.Severity;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;

public class ErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final ErrorHandler INSTANCE = new ErrorHandler();
    private static final Logger logger = LogManager.getLogger(ErrorHandler.class);

    private static final int MAX_QUEUE_SIZE = 5;
    private static final int MAX_VISIBLE_TOASTS = 3;
    private static final int MAX_WAIT_TIME_IN_MILLIS = 500;
    private final Queue<QueuedToast> toastQueue = new ArrayDeque<>();
    private VBox toastContainer;
    private volatile boolean isModalShowing = false;

    private ErrorHandler() {
    }

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof CineGradeException cgex) {
            handle(cgex);
        } else if (e.getCause() instanceof CineGradeException cgex) {
            handle(cgex);
        } else {
            handle(e);
        }
    }

    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
        Thread.currentThread().setUncaughtExceptionHandler(INSTANCE);
    }

    public void setToastContainer(VBox container) {
        this.toastContainer = container;
    }

    public void handle(CineGradeException exception) {
        logger.error("CineGradeException [{}]: {}", exception.getErrorCodeValue(), exception.getErrorMessage(), exception);

        switch (exception.getSeverity()) {
            case FATAL -> showModal(exception);
            case ERROR, WARNING ->
                    enqueueToast("[%d] %s".formatted(exception.getErrorCodeValue(),
                            exception.getSeverity()),
                            exception.getErrorMessage(),
                            exception.getSeverity());
        }
    }

    public void handle(Throwable exception) {
        if (exception instanceof CineGradeException cgex) {
            handle(cgex);
            return;
        }
        if (exception.getCause() instanceof CineGradeException cgex) {
            handle(cgex);
            return;
        }
        logger.error("Unhandled Exception: {}", exception.getMessage(), exception);
        enqueueToast("Error",
                exception.getLocalizedMessage() != null ? exception.getLocalizedMessage() : "Unexpected error",
                Severity.ERROR);
    }

    private void showModal(CineGradeException exception) {
        Platform.runLater(() -> {
            if (isModalShowing) {
                return;
            }
            isModalShowing = true;
            try {
                Platform.setImplicitExit(false);
                toastQueue.clear();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/errorModal.fxml"));
                Parent root = loader.load();

                ErrorModalController controller = loader.getController();
                controller.setupDialog(exception);

                // Preserve theme from currently active window
                Window activeWindow = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                if (activeWindow != null && activeWindow.getScene() != null && activeWindow.getScene().getRoot() != null) {
                    for (String styleClass : activeWindow.getScene().getRoot().getStyleClass()) {
                        if (styleClass.startsWith("theme-")) {
                            root.getStyleClass().add(styleClass);
                        }
                    }
                }

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
                logger.error("Failed to load or display fatal error modal", e);
            }
        });
    }

    private synchronized void enqueueToast(String title, String message, Severity severity) {
        Platform.runLater(() -> {
            if (toastContainer == null) {
                logger.warn("Toast container not registered! Message: {}", message);
                return;
            }

            if (toastQueue.size() >= MAX_QUEUE_SIZE && System.currentTimeMillis() - toastQueue.peek().pollTime >= MAX_WAIT_TIME_IN_MILLIS) {
                toastQueue.poll();
            }
            toastQueue.offer(new QueuedToast(title, message, severity, System.currentTimeMillis()));
            processToastQueue();
        });
    }

    private void processToastQueue() {
        if (toastContainer == null) return;

        while (toastContainer.getChildren().size() < MAX_VISIBLE_TOASTS && !toastQueue.isEmpty()) {
            QueuedToast item = toastQueue.poll();
            if (item == null) break;

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/errorToast.fxml"));
                Node toastNode = loader.load();

                ErrorDialogController controller = loader.getController();
                controller.setupToast(item.title(), item.message(), item.severity(), () -> {
                    // When toast finishes animating out, remove it and pop next from queue!
                    toastContainer.getChildren().remove(toastNode);
                    processToastQueue();
                });

                toastContainer.getChildren().add(toastNode);
            } catch (IOException e) {
                logger.error("Failed to load /fxmls/errorToast.fxml", e);
            }
        }
    }

    private record QueuedToast(String title, String message, Severity severity, long pollTime) {
    }
}

