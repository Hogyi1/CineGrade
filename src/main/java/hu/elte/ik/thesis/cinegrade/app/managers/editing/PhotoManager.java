package hu.elte.ik.thesis.cinegrade.app.managers.editing;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;

public class PhotoManager {

    private final ObservableList<PhotoEntry> entries = FXCollections.observableArrayList();

    public PhotoManager(Connection connection, Catalog catalog) {
    }

    public ObservableList<PhotoEntry> getPhotos() {
        return entries;
    }

    public void addPhoto(StageEntry entry) {
        entries.add(new PhotoEntry(entry.getFile()));
    }

    public void addPhoto(PhotoEntry entry) {
        entries.add(entry);
    }
}
