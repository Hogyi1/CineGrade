package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ImportStageService extends Service<List<StageEntry>> {
    @Override
    protected Task<List<StageEntry>> createTask() {
        return new Task<List<StageEntry>>() {
            @Override
            protected List<StageEntry> call() throws Exception {
                return List.of();
            }
        };
    }
}
