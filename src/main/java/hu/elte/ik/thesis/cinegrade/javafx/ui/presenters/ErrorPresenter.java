package hu.elte.ik.thesis.cinegrade.javafx.ui.presenters;

import hu.elte.ik.thesis.cinegrade.domain.enums.Severity;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorListener;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorDialogController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorModalController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public class ErrorPresenter implements ErrorListener {

    private static final Logger logger = LogManager.getLogger(ErrorPresenter.class);
    private static final int MAX_QUEUE_SIZE = 5;
    private static final int MAX_VISIBLE_TOASTS = 3;
    private static final int MAX_WAIT_TIME_IN_MILLIS = 500;
    private final Queue<ErrorPresenter.QueuedToast> toastQueue = new ArrayDeque<>(MAX_QUEUE_SIZE);
    private final VBox toastContainer;
    private volatile boolean isModalShowing = false;

    public ErrorPresenter(VBox toastContainer) {
        this.toastContainer = toastContainer;
        ErrorHandler.getInstance().registerListener(this);
    }

    private void showModal(CineGradeException exception) {
        if (isModalShowing) return;
        isModalShowing = true;
        Platform.runLater(toastQueue::clear);
        showFatalErrorModal(exception);
    }

    private void enqueueToast(String title, String message, Severity severity) {
        Platform.runLater(() -> {
            if (toastContainer == null) {
                logger.warn("Toast container not registered! Message: {}", message);
                return;
            }

            if (toastQueue.size() >= MAX_QUEUE_SIZE) {
                if (System.currentTimeMillis() - toastQueue.peek().pollTime >= MAX_WAIT_TIME_IN_MILLIS) {
                    toastQueue.poll();
                }
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

    @Override
    public void onError(CineGradeException exception, ErrorHandler.ErrorDisplayMode displayMode) {
        switch (displayMode) {
            case MODAL -> showModal(exception);
            case TOAST -> enqueueToast("[%d] %s".formatted(exception.getErrorCodeValue(), exception.getSeverity()), exception.getLocalizedMessage(), exception.getSeverity());
        }
    }

    public void showFatalErrorModal(CineGradeException exception) {
        Platform.runLater(() -> {
            try {
                Platform.setImplicitExit(false);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/errorModal.fxml"));
                Parent root = loader.load();
                ErrorModalController controller = loader.getController();
                controller.setupDialog(exception);

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

    private record QueuedToast(String title, String message, Severity severity, long pollTime) {
    }
}
