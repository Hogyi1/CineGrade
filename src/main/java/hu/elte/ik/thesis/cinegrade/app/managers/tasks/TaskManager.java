package hu.elte.ik.thesis.cinegrade.app.managers.tasks;

import hu.elte.ik.thesis.cinegrade.app.managers.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;

public class TaskManager {

    private final ThreadPoolManager threadPoolManager;
    private final ObservableList<Service<?>> completedServices;
    private final ObservableList<Service<?>> runningServices;

    public TaskManager(ThreadPoolManager threadPoolManager) {
        this.threadPoolManager = threadPoolManager;
        this.runningServices = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
        this.completedServices = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    public void configure(Service<?> service) {
        threadPoolManager.setMainExecutorService(service);

        runningServices.add(service);

        service.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, e -> {
            Throwable error = service.getException();
            if (error != null){
                ErrorHandler.getInstance().handle(error);
            }
        });

        service.stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SCHEDULED, RUNNING -> {
                    if (!runningServices.contains(service)) {
                        runningServices.add(service);
                    }
                }
                case SUCCEEDED, FAILED, CANCELLED -> {
                    runningServices.remove(service);
                    if (!completedServices.contains(service)) {
                        completedServices.add(service);
                    }
                }
                default -> {}
            }
        });
    }

    public ObservableList<Service<?>> runningTasks() {
        return runningServices;
    }

    public ObservableList<Service<?>> completedTasks() {
        return completedServices;
    }
}
