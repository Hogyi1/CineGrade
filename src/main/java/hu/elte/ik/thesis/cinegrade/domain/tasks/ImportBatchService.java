package hu.elte.ik.thesis.cinegrade.domain.tasks;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ImportBatchService extends Service<Void> {

    public ImportBatchService() {

    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                return null;
            }
        };
    }
}
