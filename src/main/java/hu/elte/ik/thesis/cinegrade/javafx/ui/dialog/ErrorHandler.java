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
import java.util.Queue;

//TODO REVIEW CODE
public class ErrorHandler {

    private static final ErrorHandler INSTANCE = new ErrorHandler();
    private static final Logger logger = LogManager.getLogger(ErrorHandler.class);

    private static final int MAX_QUEUE_SIZE = 5;
    private static final int MAX_VISIBLE_TOASTS = 3;

    private record QueuedToast(String title, String message, Severity severity) {}

    private final Queue<QueuedToast> toastQueue = new ArrayDeque<>();
    private VBox toastContainer;

    private ErrorHandler() {}

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    public void setToastContainer(VBox container) {
        this.toastContainer = container;
    }

    public void handle(CineGradeException exception) {
        logger.error("CineGradeException [{}]: {}", exception.getErrorCodeValue(), exception.
                getErrorMessage(), exception);

        switch (exception.getSeverity()) {
            case FATAL -> showModal(exception);
            case ERROR, WARNING -> enqueueToast(
                    "[%d] %s".formatted(exception.getErrorCodeValue(), exception.getSeverity()),
                    exception.getErrorMessage(),
                    exception.getSeverity()
            );
        }
    }

    public void handle(Exception exception) {
        logger.error("Unhandled Exception: {}", exception.getMessage(), exception);
        enqueueToast("Error", exception.getLocalizedMessage() != null ? exception.getLocalizedMessage()
                : "Unexpected error", Severity.ERROR);
    }

    // ==========================================
    // Modal Handling (FATAL)
    // ==========================================
    private void showModal(CineGradeException exception) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/errorModal.fxml"));
                Parent root = loader.load();

                ErrorModalController controller = loader.getController();
                controller.setupDialog(exception);

                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.initStyle(StageStyle.UNDECORATED);
                modalStage.setScene(new Scene(root));

                Window activeWindow = Window.getWindows().stream().filter(Window::isShowing).
                        findFirst().orElse(null);
                if (activeWindow != null) {
                    modalStage.initOwner(activeWindow);
                }

                modalStage.showAndWait();
            } catch (IOException e) {
                logger.error("Failed to load /fxmls/errorModal.fxml", e);
            }
        });
    }

    // ==========================================
    // Toast Queue Handling (ERROR / WARNING)
    // ==========================================
    private synchronized void enqueueToast(String title, String message, Severity severity) {
        Platform.runLater(() -> {
            if (toastContainer == null) {
                logger.warn("Toast container not registered! Message: {}", message);
                return;
            }

            if (toastQueue.size() >= MAX_QUEUE_SIZE) {
                toastQueue.poll(); // Drop oldest waiting toast
            }
            toastQueue.offer(new QueuedToast(title, message, severity));
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
}

