package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.app.managers.editing.EditingManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ExportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.PhotoManager;
import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.database.CatalogDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.PreviewDatabase;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.database.CatalogService;

public class CatalogScopeFactory {

    private final TaskManager taskManager;

    public CatalogScopeFactory(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public CatalogScope open(Catalog catalog) {
        ResourceStack resources = new ResourceStack();
        try {
            CatalogDatabase db = resources.push(new CatalogDatabase(catalog.getProjectFilePath()));
            PreviewDatabase previewDb = resources.push(new PreviewDatabase(catalog.getPreviewDatabasePath()));

            CatalogService catalogService = new CatalogService(db.getConnection());

            PhotoManager photoManager = resources.push(new PhotoManager(db.getConnection(), catalog));
            ImportManager importManager = resources.push(new ImportManager(db.getConnection(), catalog, taskManager, photoManager));
            ExportManager exportManager = resources.push(new ExportManager(db.getConnection(), catalog));
            EditingManager editingManager = resources.push(new EditingManager(db.getConnection(), catalog));

            return new CatalogScope(resources, catalog, catalogService, importManager, editingManager, exportManager, photoManager);
        } catch (Exception e) {
            resources.close();
            throw new RuntimeException("Failed to open catalog scope", e);
        }
    }
}
