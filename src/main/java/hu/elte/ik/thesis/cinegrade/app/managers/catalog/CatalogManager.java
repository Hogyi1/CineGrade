package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.app.managers.concurrency.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.app.scope.CatalogScope;
import hu.elte.ik.thesis.cinegrade.app.scope.CatalogScopeFactory;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.database.CatalogService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.database.RecentCatalogService;
import hu.elte.ik.thesis.cinegrade.infra.services.storage.StorageService;
import hu.elte.ik.thesis.cinegrade.infra.utils.format.FormatUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class CatalogManager implements AutoCloseable {

    public static final String PROJECT_EXTENSION = ".cgproj";
    private static final byte[] SQLITE_HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final Logger logger = LogManager.getLogger(CatalogManager.class);
    private final Path fallBackPath = Paths.get(AppConfig.INSTANCE.getProjectDirectory());
    private final RecentCatalogService recentCatalogService;
    private final ObservableList<Catalog> catalogObserVableList = FXCollections.observableArrayList();
    private final StorageService storageService;
    private final CatalogScopeFactory scopeFactory;

    @Getter
    private CatalogScope currentScope;

    public CatalogManager(CatalogScopeFactory scopeFactory, StorageService storageService) {
        recentCatalogService = new RecentCatalogService();
        this.storageService = storageService;
        this.scopeFactory = scopeFactory;
        refreshCatalogs();
    }

    /**
     * Removes a catalog from the recent list.
     *
     * @param catalog the catalog to remove
     */
    public void removeFromRecent(Catalog catalog) {
        boolean isRemoved = recentCatalogService.removeCatalog(catalog);
        if (isRemoved) {
            catalogObserVableList.remove(catalog);
            logger.info("Catalog is removed from recents: {}", catalog.getCatalogName());
        } else {
            logger.warn("Couldn't remove catalog from recents: {} with id: {}", catalog.getCatalogName(), catalog.getId());
            throw new CineGradeException(ErrorCode.CATALOG_REMOVE_FAILED, catalog.getCatalogName());
        }
    }

    /**
     * Deletes a catalog. Only closes the open catalog if it is the one being deleted.
     *
     * @param catalog the catalog to delete
     */
    public void deleteCatalog(Catalog catalog) {
        if (currentScope != null && currentScope.catalog().getId() == catalog.getId()) {
            closeCurrentScope();
        }

        boolean isDeleted = storageService.deleteDirectory(catalog.getProjectDirectory());
        if (isDeleted) {
            removeFromRecent(catalog);
            logger.info("Catalog is deleted: {}", catalog.getCatalogName());
        } else {
            throw new CineGradeException(ErrorCode.CATALOG_DELETE_FAILED, catalog.getProjectDirectory());
        }
    }

    /**
     * Returns the list of available catalogs.
     *
     * @return the list of catalogs
     */
    public ObservableList<Catalog> getCatalogs() {
        return catalogObserVableList;
    }

    /**
     * Opens a catalog from the recent list.
     *
     * @param catalog the catalog to open
     */
    public void openCatalog(Catalog catalog) {
        if (catalog == null) {
            return;
        }
        openProject(catalog.getProjectFilePath());
    }

    /**
     * Opens a catalog from a .cgproj file.
     *
     * @param file the .cgproj file to open
     */
    public void openCatalog(File file) {
        if (file == null) {
            throw new CineGradeException(ErrorCode.FILE_NOT_FOUND, "Selected file is null");
        }
        openProject(file.toPath());
    }

    /**
     * Opens a project.
     *
     * @param projectFilePath the path to the project file
     */
    private void openProject(Path projectFilePath) {
        Path cgprojFile = projectFilePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(cgprojFile)) {
            throw new CineGradeException(ErrorCode.CATALOG_NOT_FOUND, cgprojFile);
        }
        if (!validateProject(cgprojFile)) {
            throw new CineGradeException(ErrorCode.CATALOG_CORRUPTED, cgprojFile);
        }

        Catalog catalog = recentCatalogService.findOrAddCatalog(FormatUtils.stripExtension(cgprojFile), cgprojFile);
        recentCatalogService.updateLastOpenedAt(catalog.getId(), Instant.now());
        ensureDirectories(catalog.getProjectDirectory());

        openScope(catalog, WorkspacePanel.DEVELOPMENT);
        logger.info("Catalog opened successfully: {}", cgprojFile);

        refreshCatalogs();
    }

    /**
     * Creates a new project with the specified name and path.
     *
     * @param name the name of the project
     * @param path the parent directory, the project gets its own folder inside it
     */
    public void createNewProject(String name, Path path) {
        Path projectDirectory = path.resolve(name).toAbsolutePath().normalize();
        Path projectFilePath = projectDirectory.resolve(name + PROJECT_EXTENSION);

        // Check if project already exists there
        if (Files.exists(projectFilePath)) {
            throw new CineGradeException(ErrorCode.CATALOG_ALREADY_EXISTS, projectFilePath.toString());
        }

        // Setup project directories, the database files are created inside them when the scope opens
        ensureDirectories(projectDirectory);

        // findOrAdd: a stale recents row may still point to this file if it was deleted by hand
        Catalog catalog = recentCatalogService.findOrAddCatalog(name, projectFilePath);
        try {
            openScope(catalog, WorkspacePanel.IMPORT);
        } catch (RuntimeException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, projectFilePath.toString());
        }
        logger.info("New project created: {}", projectFilePath);

        // Refresh the list
        refreshCatalogs();
    }

    /**
     * Closes the open catalog (if any), opens the scope of the given one, records
     * its current location in catalog_info and loads the panel it was left on.
     *
     * @param fallbackPanel the panel to start on if the catalog has none stored yet
     */
    private void openScope(Catalog catalog, WorkspacePanel fallbackPanel) {
        closeCurrentScope();

        currentScope = scopeFactory.open(catalog);
        try {
            CatalogService catalogService = currentScope.catalogService();
            catalogService.setMetaData(catalog);
            catalog.setLastPanel(catalogService.getLastPanel().orElse(fallbackPanel));
        } catch (RuntimeException ex) {
            closeCurrentScope();
            throw ex;
        }
    }

    private void closeCurrentScope() {
        try (CatalogScope scope = currentScope) {
            if (scope != null) {
                logger.info("Closing catalog: {}", scope.catalog().getCatalogName());
            }
        } finally {
            currentScope = null;
        }
    }

    private void ensureDirectories(Path projectDirectory) {
        try {
            storageService.ensureDirectory(projectDirectory);
            storageService.ensureDirectory(projectDirectory.resolve("temp"));
            storageService.ensureDirectory(projectDirectory.resolve("temp").resolve("imports"));
            storageService.ensureDirectory(projectDirectory.resolve("settings"));
            storageService.ensureDirectory(projectDirectory.resolve("imports"));
            storageService.ensureDirectory(projectDirectory.resolve("media"));
            storageService.ensureDirectory(projectDirectory.resolve("cache").resolve("thumbnails"));
        } catch (CineGradeException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, projectDirectory.toString());
        }
    }

    /**
     * Returns the default path for the catalog.
     *
     * @return The default path for the catalog.
     */
    public Path getDefaultPath() {
        Optional<String> savedPath = recentCatalogService.getSavedPath();
        return savedPath.map(Path::of).filter(Files::isDirectory).orElse(fallBackPath);
    }

    /**
     * Refresh the catalogs that can be shown.
     * Get the catalogs from db and show them
     */
    public void refreshCatalogs() {
        Platform.runLater(() -> {
            List<Catalog> recentCatalogs = recentCatalogService.getRecentCatalogs().stream().peek(this::calculateCatalogSize).toList();
            catalogObserVableList.setAll(recentCatalogs);
        });
    }

    /**
     * Validate the project: it has to be a .cgproj file whose first 16 bytes
     * are the SQLite header.
     *
     * @param cgprojFile The .cgproj file.
     * @return true if the project is valid, false otherwise.
     */
    public boolean validateProject(Path cgprojFile) {
        if (cgprojFile == null || !cgprojFile.getFileName().toString().toLowerCase().endsWith(PROJECT_EXTENSION)) {
            logger.warn("Not a {} file: {}", PROJECT_EXTENSION, cgprojFile);
            return false;
        }

        // Read the first 16 bytes of the file and compare it to the SQLite header
        try (InputStream in = Files.newInputStream(cgprojFile)) {
            byte[] header = in.readNBytes(16);
            if (!Arrays.equals(header, SQLITE_HEADER)) {
                logger.warn("File is not a valid SQLite database: {}", cgprojFile);
                return false;
            }
        } catch (IOException e) {
            logger.warn("Failed to read SQLite header from {}: {}", cgprojFile, e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Calculate the catalogs size based on the parent directory if it's possible
     * Otherwise calculate the sqlite database size
     *
     */
    private void calculateCatalogSize(Catalog catalog) {
        if (catalog == null || catalog.getProjectDirectory() == null) {
            logger.warn("Cannot calculate catalog size: catalog or catalog path is null");
            return;
        }

        try (Stream<Path> paths = Files.walk(catalog.getProjectDirectory())) {
            long size = paths
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
            catalog.setSize(size);
            logger.debug("Calculated catalog size for '{}': {} bytes, started from: {}", catalog.getCatalogName(), size, catalog.getProjectDirectory());
        } catch (IOException ex) {
            logger.warn("Failed to calculate catalog size for '{}' at {}: {}", catalog.getCatalogName(), catalog.getProjectDirectory(), ex.getMessage(), ex);
        }

    }

    @Override
    public void close() {
        closeCurrentScope();
    }
}
