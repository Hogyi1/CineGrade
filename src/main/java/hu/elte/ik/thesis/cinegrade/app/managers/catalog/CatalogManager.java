package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.services.database.RecentCatalogService;
import hu.elte.ik.thesis.cinegrade.infra.services.database.UserService;
import javafx.collections.ObservableList;

import java.nio.file.Path;

public class CatalogManager {

    private RecentCatalogService recentCatalogService;
    private ObservableList<Catalog> catalogObserVableList;

    public CatalogManager() {
        recentCatalogService = new RecentCatalogService();
    }

    public void removeFromRecent(Catalog catalog){

    }

    public void deleteCatalog(Catalog catalog) {

    }

    public ObservableList<Catalog> getCatalogs() {
        return null;
    }

    public void openCatalog(Catalog newValue) {
    }

    public void openCatalog(Path path) {
    }
}
