package hu.elte.ik.thesis.cinegrade.domain.tasks;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.ImportRequest;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ImportService extends Service<List<PhotoEntry>> {

    private final List<ImportRequest> stageEntries;
    private final ImportMethod method;
    private final Catalog catalog;

    public ImportService(List<ImportRequest> stageEntries, ImportMethod method, Catalog catalog) {
        this.stageEntries = stageEntries;
        this.method = method;
        this.catalog = catalog;
    }

    @Override
    protected Task<List<PhotoEntry>> createTask() {
        return new Task<List<PhotoEntry>>() {
            @Override
            protected List<PhotoEntry> call() throws Exception {
                return null;
            }
        };
    }
}
