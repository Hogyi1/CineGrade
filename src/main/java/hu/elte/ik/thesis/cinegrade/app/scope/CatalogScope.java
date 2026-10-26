package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.app.managers.editing.EditingManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ExportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.PhotoManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.database.CatalogService;

public record CatalogScope(ResourceStack resources,
                           Catalog catalog,
                           CatalogService catalogService,
                           ImportManager importManager,
                           EditingManager editingManager,
                           ExportManager exportManager,
                           PhotoManager photoManager) implements AutoCloseable {

    @Override
    public void close() {
        resources.close();
    }
}
