package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.app.managers.editing.EditingManager;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.ExportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.imports.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.imports.StagingArea;
import hu.elte.ik.thesis.cinegrade.app.managers.editing.PhotoManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.ThumbnailService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.database.CatalogService;

public record CatalogScope(ResourceStack resources,
                           Catalog catalog,
                           ThumbnailService thumbnailService,
                           CatalogService catalogService,
                           StagingArea stagingArea,
                           ImportManager importManager,
                           EditingManager editingManager,
                           ExportManager exportManager,
                           PhotoManager photoManager) implements AutoCloseable {

    @Override
    public void close() {
        resources.close();
    }
}
