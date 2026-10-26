package hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks;

import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.ThreadPoolManager;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class TaskManager implements AutoCloseable {

    private final ExecutorService servicePool;
    @Getter
    private final ObservableList<Service<?>> completedServices;
    @Getter
    private final ObservableList<Service<?>> runningServices;

    private final ConcurrentHashMap<Object, List<Service<?>>> groups = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger(TaskManager.class);

    public TaskManager(ExecutorService servicePool) {
        this.servicePool = servicePool;
        // Plain lists: configure() runs on the FX thread and Service state changes are always delivered there
        this.runningServices = FXCollections.observableArrayList();
        this.completedServices = FXCollections.observableArrayList();
    }

    /**
     * Configures a service for task management
     * @param service the service to configure
     */
    public void configure(Service<?> service) {
        service.setExecutor(servicePool);

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
                        logger.debug("Task started: {}", service);
                    }
                }
                case SUCCEEDED, FAILED, CANCELLED -> {
                    runningServices.remove(service);
                    if (!completedServices.contains(service)) {
                        completedServices.add(service);
                        logger.debug("Task completed: {}", service);
                    }
                }
                default -> {}
            }
        });
    }

    public void configure(Service<?> service, Object group) {
        configure(service);
        List<Service<?>> list = groups.computeIfAbsent(group, k -> new CopyOnWriteArrayList<>());
        list.add(service);
        service.stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SUCCEEDED, FAILED, CANCELLED -> list.remove(service);
                default -> {}
            }
        });
    }

    public void cancelGroup(Object group) {
        List<Service<?>> servicesToCancel = groups.get(group);
        if (servicesToCancel != null) {
            servicesToCancel.forEach(Service::cancel);
            logger.debug("Tasks cancelled for group: {}", group);
        }
    }

    @Override
    public void close() throws Exception {
        new ArrayList<>(runningServices).forEach(Service::cancel);
    }
}
