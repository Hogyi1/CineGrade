package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.services.database.RecentCatalogService;

public class CatalogManager {

    private RecentCatalogService recentCatalogService;
    public CatalogManager() {
        recentCatalogService = new RecentCatalogService();
    }

    public void removeFromRecent(Catalog catalog){

    }

    public void deleteCatalog(Catalog catalog) {

    }
}
