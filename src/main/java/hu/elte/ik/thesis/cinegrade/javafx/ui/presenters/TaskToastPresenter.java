package hu.elte.ik.thesis.cinegrade.javafx.ui.presenters;

import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.TaskToastController;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

/**
 * Shows one toast per running service of the TaskManager.
 * At most MAX_VISIBLE_TOASTS are on screen, the rest wait in the queue.
 * A toast is finished (outcome + slide-out) when its service leaves the running list,
 * and its node is removed from the container only after the slide-out animation.
 */
public class TaskToastPresenter {

    private static final Logger logger = LogManager.getLogger(TaskToastPresenter.class);
    private static final String TOAST_FXML = "/fxmls/taskToast.fxml";
    private static final int MAX_VISIBLE_TOASTS = 3;

    private final VBox toastContainer;
    private final Queue<Service<?>> toastQueue = new ArrayDeque<>();
    private final HashMap<Service<?>, TaskToastController> visibleToasts = new HashMap<>();

    public TaskToastPresenter(TaskManager taskManager, VBox toastContainer) {
        this.toastContainer = toastContainer;

        taskManager.getRunningServices().addListener((ListChangeListener<Service<?>>) change -> {
            List<Service<?>> removed = new ArrayList<>();
            List<Service<?>> added = new ArrayList<>();
            while (change.next()) {
                removed.addAll(change.getRemoved());
                added.addAll(change.getAddedSubList());
            }
            Platform.runLater(() -> onRunningServicesChanged(removed, added));
        });
    }

    private void onRunningServicesChanged(List<Service<?>> removed, List<Service<?>> added) {
        for (Service<?> service : removed) {
            toastQueue.remove(service);
            TaskToastController toast = visibleToasts.remove(service);
            if (toast != null) {
                toast.finish(outcomeOf(service));
            }
        }

        for (Service<?> service : added) {
            if (!visibleToasts.containsKey(service) && !toastQueue.contains(service)) {
                toastQueue.offer(service);
            }
        }

        processToastQueue();
    }

    private void processToastQueue() {
        while (toastContainer.getChildren().size() < MAX_VISIBLE_TOASTS && !toastQueue.isEmpty()) {
            Service<?> service = toastQueue.poll();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(TOAST_FXML));
                Node toastNode = loader.load();

                TaskToastController controller = loader.getController();
                controller.setupToast(service.messageProperty(), service.progressProperty(),
                        service::cancel,
                        () -> {
                            toastContainer.getChildren().remove(toastNode);
                            processToastQueue();
                        }
                );

                visibleToasts.put(service, controller);
                toastContainer.getChildren().add(toastNode);
            } catch (IOException e) {
                logger.error("Failed to load {}", TOAST_FXML, e);
            }
        }
    }

    private static TaskToastController.Outcome outcomeOf(Service<?> service) {
        return switch (service.getState()) {
            case SUCCEEDED -> TaskToastController.Outcome.SUCCEEDED;
            case CANCELLED -> TaskToastController.Outcome.CANCELLED;
            default -> TaskToastController.Outcome.FAILED;
        };
    }
}
