package hu.elte.ik.thesis.cinegrade.app.managers.editing;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.infra.database.TransactionRunner;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.PhotoDao;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.ThumbnailService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public class PhotoManager implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(PhotoManager.class);

    private final ObservableList<PhotoEntry> allPhotos = FXCollections.observableArrayList();
    private final ObservableList<PhotoEntry> lastImportPhotos = FXCollections.observableArrayList();

    private final PhotoDao photoDao;
    private final TransactionRunner catalogTx;
    private final ThumbnailService thumbnailService;
    private final Catalog catalog;

    public PhotoManager(TransactionRunner catalogTx, PhotoDao photoDao, ThumbnailService thumbnailService, Catalog catalog) {
        this.catalogTx = catalogTx;
        this.thumbnailService = thumbnailService;
        this.photoDao = photoDao;
        this.catalog = catalog;

        loadCatalogPhotos();
    }

    /**
     * Starts a new import session by clearing the previous import list.
     */
    public void startImportSession() {
        Platform.runLater(lastImportPhotos::clear);
    }

    public void addAll(List<PhotoEntry> photoEntries) {
        if (photoEntries != null && !photoEntries.isEmpty()) {
            allPhotos.addAll(photoEntries);
        }
    }

    /**
     * Loads existing photos from the catalog database into memory at startup.
     */
    public void loadCatalogPhotos() {
        try {
            // Blocks until the db thread answers; runs once when the catalog is opened
            List<PhotoEntry> existingPhotos = catalogTx.read(c -> photoDao.findAll()).join();
            if (existingPhotos != null) {

                Platform.runLater(() -> allPhotos.setAll(existingPhotos));
                logger.info("Loaded {} existing photo(s) from catalog database", existingPhotos.size());

                Optional<PhotoEntry> lastImported = allPhotos.stream().max(Comparator.comparing(PhotoEntry::getDateImported));
                if (lastImported.isPresent()) {
                    String importId = lastImported.get().getImportId();
                    List<PhotoEntry> lastImportedPhotos = allPhotos.stream().filter(photo -> importId.equals(photo.getImportId())).toList();

                    Platform.runLater(() -> lastImportPhotos.setAll(lastImportedPhotos));
                }
            }
        } catch (CompletionException e) {
            logger.error("Failed to load photos from catalog database", e.getCause());
        }
    }

    /**
     * Called progressively by ImportService as each batch of photos is committed.
     */
    public void addImportedBatch(List<PhotoEntry> batch) {
        if (batch == null || batch.isEmpty()) return;
        Platform.runLater(() -> {
            lastImportPhotos.addAll(batch);
            allPhotos.addAll(batch);
            logger.debug("Added batch of {} photos. Total in catalog: {}", batch.size(), allPhotos.size());
        });
    }

    /**
     * Updates the rating of a photo in the database.
     *
     * @param photo  the photo entry to update
     * @param rating the new rating
     */
    public void updateRating(PhotoEntry photo, int rating) {
        photo.setRating(rating);
        catalogTx.execute(c -> photoDao.updateRating(photo.getId(), rating))
                .exceptionally(ex -> {
                    logger.error("Failed to update rating in database for photo {}", photo.getId(), ex);
                    return null;
                });
    }

    /**
     * Updates the color label of a photo in the database.
     *
     * @param photo      the photo entry to update
     * @param colorLabel the new color label
     */
    public void updateColorLabel(PhotoEntry photo, String colorLabel) {
        photo.setColorLabel(colorLabel);
        catalogTx.execute(c -> photoDao.updateColorLabel(photo.getId(), colorLabel))
                .exceptionally(ex -> {
                    logger.error("Failed to update color label in database for photo {}", photo.getId(), ex);
                    return null;
                });
    }

    /**
     * Removes a photo from the catalog and the database.
     *
     * @param photo the photo entry to remove
     */
    public void removePhoto(PhotoEntry photo) {
        if (photo == null) return;

        Platform.runLater(() -> {
            allPhotos.remove(photo);
            lastImportPhotos.remove(photo);
        });

        // No foreign key across the two database files: the blob is removed once the row is gone
        catalogTx.execute(c -> photoDao.delete(photo.getId()))
                .thenRun(() -> thumbnailService.delete(photo.getId()))
                .exceptionally(ex -> {
                    logger.error("Failed to delete photo {} from database", photo.getId(), ex);
                    return null;
                });
    }

    public ObservableList<PhotoEntry> getPhotos() {
        return allPhotos;
    }

    @Override
    public void close() {
        allPhotos.clear();
        lastImportPhotos.clear();
    }
}
