package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.infra.services.database.CatalogDbService;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.nio.file.Path;

public class CatalogManager {

    private CatalogDbService catalogDbService;
    private ObservableList<Catalog> catalogObserVableList;

    public CatalogManager() {
        catalogDbService = new CatalogDbService();
    }

    public void removeFromRecent(Catalog catalog) {

    }

    public void deleteCatalog(Catalog catalog) {

    }

    public ObservableList<Catalog> getCatalogs() {
        return null;
    }

    public void openCatalog(Catalog newValue) {
    }

    public void openCatalog(File file) {
    }

    public void createNewProject() {
    }
}
