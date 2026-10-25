package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.app.managers.editing.EditingManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ExportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.PhotoManager;
import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.storage.database.CatalogDatabase;
import hu.elte.ik.thesis.cinegrade.infra.storage.database.PreviewDatabase;

public class CatalogScopeFactory {

    private final TaskManager taskManager;

    public CatalogScopeFactory(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public CatalogScope open(Catalog catalog) {
        ResourceStack resources = new ResourceStack();
        try {
            CatalogDatabase db = new CatalogDatabase(catalog.getProjectFilePath());
            PreviewDatabase previewDb = new PreviewDatabase(catalog.getPreviewDatabasePath());
            resources.push(db);

            PhotoManager photoManager = new PhotoManager(db.getConnection(), catalog);
            resources.push(photoManager);

            ImportManager importManager = new ImportManager(db.getConnection(), catalog, taskManager, photoManager);
            resources.push(importManager);

            ExportManager exportManager = new ExportManager(db.getConnection(), catalog);
            resources.push(exportManager);

            EditingManager editingManager = new EditingManager(db.getConnection(), catalog);
            resources.push(editingManager);

            return new CatalogScope(resources, catalog);
        } catch (Exception e) {
            resources.close();
            throw new RuntimeException("Failed to open catalog scope", e);
        }
    }
}
