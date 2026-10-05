package hu.elte.ik.thesis.cinegrade.domain.context;

import hu.elte.ik.thesis.cinegrade.app.managers.catalog.CatalogManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.*;
import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;

import java.sql.Connection;

public class CatalogContext {
    public final Catalog catalog;
    public final Connection connection;
    public final PhotoManager photoManager;
    public final CatalogManager catalogManager;
    public final ImportManager importManager;
    public final EditingManager editingManager;
    public final ExportManager exportManager;
    public final TagManager tagManager;
    public final TaskManager taskManager;
    public final WorkspacePanel startingPanel;

    public CatalogContext(Connection connection, Catalog catalog, CatalogManager catalogManager, TaskManager taskManager, WorkspacePanel startingPanel) {
        this.catalog = catalog;
        this.connection = connection;
        this.photoManager = new PhotoManager(connection, catalog);
        this.catalogManager = catalogManager;
        this.importManager = new ImportManager(connection, catalog, taskManager);
        this.editingManager = new EditingManager(connection, catalog);
        this.exportManager = new ExportManager(connection, catalog);
        this.tagManager = new TagManager(connection, catalog);
        this.taskManager = taskManager;
        this.startingPanel = startingPanel;
    }
}
