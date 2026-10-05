package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.app.managers.tasks.TaskManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.database.CatalogDatabase;
import hu.elte.ik.thesis.cinegrade.infra.services.database.CatalogService;
import hu.elte.ik.thesis.cinegrade.infra.services.database.RecentCatalogService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CatalogManager {

    private static final byte[] SQLITE_HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final Logger logger = LogManager.getLogger(CatalogManager.class);
    private static final int MAX_DELETE_DEPTH = 8;
    private final Path fallBackPath = Paths.get(AppConfig.INSTANCE.getProjectDirectory());
    private final RecentCatalogService recentCatalogService;
    private final ObservableList<Catalog> catalogObserVableList = FXCollections.observableArrayList();
    private final TaskManager taskManager;
    private CatalogService catalogService;
    private CatalogContext currentCatalogContext;

    public CatalogManager(TaskManager taskManager) {
        recentCatalogService = new RecentCatalogService();
        this.taskManager = taskManager;
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
     * Deletes a catalog.
     *
     * @param catalog the catalog to delete
     */
    public void deleteCatalog(Catalog catalog) {
        Path catalogPath = Path.of(catalog.getCatalogPath());
        CatalogDatabase.getInstance().closeConnection();

        if (validatePath(catalogPath)) {
            boolean isDeleted = deleteRecursively(catalogPath, 0);
            if (isDeleted) {
                removeFromRecent(catalog);
                logger.info("Catalog is deleted: {}", catalog.getCatalogName());
                return;
            }
        }
        throw new CineGradeException(ErrorCode.CATALOG_DELETE_FAILED, catalogPath);
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
     * Opens a catalog.
     *
     * @param catalog the catalog to open
     */
    public void openCatalog(Catalog catalog) {
        if (catalog == null) {
            return;
        }
        Path path = Path.of(catalog.getCatalogPath());
        openProject(path, catalog.getCatalogName(), catalog.getId());
    }

    /**
     * Opens a catalog from a file.
     *
     * @param file the file to open
     */
    public void openCatalog(File file) {
        if (file == null) {
            throw new CineGradeException(ErrorCode.FILE_NOT_FOUND, "Selected file is null");
        }
        openCatalog(file.toPath());
    }

    /**
     * Opens a catalog from a path.
     *
     * @param path the path to the catalog
     */
    public void openCatalog(Path path) {
        if (path == null) {
            return;
        }
        String name = path.getFileName().toString();
        if (name.endsWith(".cgproj")) {
            name = name.substring(0, name.length() - ".cgproj".length());
        }
        openProject(path, name, null);
    }

    private void openProject(Path path, String name, Integer catalogId) {
        if (!validateProject(path, name)) {
            throw new CineGradeException(ErrorCode.CATALOG_CORRUPTED, path.toAbsolutePath().toString());
        }

        var cgprojFile = resolveCgprojPath(path, name).orElse(null);

        try {
            CatalogDatabase.getInstance().openConnection(cgprojFile);
            Connection connection = CatalogDatabase.getInstance().getConnection();
            catalogService = new CatalogService(connection);
            catalogService.setLastOpenedAt(Instant.now());

            Path dirPath = cgprojFile.getParent() != null ? cgprojFile.getParent() : cgprojFile;
            String normalizedDirPath = dirPath.toAbsolutePath().normalize().toString();

            Optional<Catalog> existingInRecents = catalogObserVableList.stream()
                    .filter(c -> (catalogId != null && c.getId() == catalogId) ||
                            (c.getCatalogPath() != null && Path.of(c.getCatalogPath()).toAbsolutePath().normalize().toString().equalsIgnoreCase(normalizedDirPath)))
                    .findFirst();

            // Mismatching recent list and database on ids
            Catalog catalog;
            if (existingInRecents.isPresent()) {
                Catalog existing = existingInRecents.get();
                recentCatalogService.updateLastOpenedAt(existing.getId(), Instant.now());
                catalogService.setSetting(CatalogService.KEY_RECENT_CATALOG_ID, String.valueOf(existing.getId()));
                catalog = existing;
            } else {
                Catalog newRecent = recentCatalogService.addCatalog(name, dirPath);
                catalogService.setMetaData(newRecent);
                catalog = newRecent;
            }

            this.currentCatalogContext = new CatalogContext(connection, catalog, this, taskManager, WorkspacePanel.DEVELOPMENT);
            logger.info("Catalog opened successfully: {}", cgprojFile);
        } catch (CineGradeException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CORRUPTED, ex, path.toString());
        }

        refreshCatalogs();
    }

    /**
     * Creates a new project with the specified name and path.
     *
     * @param name the name of the project
     * @param path the path where the project will be created
     */
    public void createNewProject(String name, Path path) {
        Path projectDirectory = path.resolve(name);

        // Check if project already exists there
        if (Files.exists(projectDirectory.resolve(name + ".cgproj"))) {
            throw new CineGradeException(ErrorCode.CATALOG_ALREADY_EXISTS, projectDirectory.toString());
        }

        // Setup project directories
        try {
            Files.createDirectories(projectDirectory);
            Files.createDirectories(projectDirectory.resolve("temp"));
            Files.createDirectories(projectDirectory.resolve("settings"));
        } catch (IOException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, projectDirectory.toString());
        }

        // Open up catalog connection
        Path dbFile = projectDirectory.resolve(name + ".cgproj");
        try {
            CatalogDatabase.getInstance().openConnection(dbFile);
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, dbFile.toString());
        }

        Connection connection = CatalogDatabase.getInstance().getConnection();
        // Add to recent catalogs
        Catalog catalog = recentCatalogService.addCatalog(name, projectDirectory);
        // Save project metadata first
        catalogService = new CatalogService(connection);
        catalogService.setMetaData(catalog);
        // Create the context for that catalog
        this.currentCatalogContext = new CatalogContext(connection, catalog, this, taskManager, WorkspacePanel.IMPORT);
        logger.info("New project created: {}", projectDirectory);

        // Refresh the list
        refreshCatalogs();
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
    private void refreshCatalogs() {
        Platform.runLater(() -> {
            List<Catalog> recentCatalogs = recentCatalogService.getRecentCatalogs().stream().peek(this::calculateCatalogSize).toList();
            catalogObserVableList.setAll(recentCatalogs);
        });
    }

    /**
     * Validate the project by reading the first 16 bytes
     * of the projects header and compare it to the SQLite header.
     *
     * @param path The path to the project directory or .cgproj file.
     * @return true if the project is valid, false otherwise.
     *
     */
    public boolean validateProject(Path path, String name) {
        // Get path resolved to .cgproj file
        Optional<Path> result = resolveCgprojPath(path, name);
        if (result.isEmpty()) {
            return false;
        }

        Path cgprojFile = result.get();

        if (!validatePath(cgprojFile)) {
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
     * Resolves the path to a .cgproj file based on the provided path and catalog name.
     *
     * @param path The base path to resolve from.
     * @return The resolved path to the .cgproj file, or null if not found.
     */
    private Optional<Path> resolveCgprojPath(Path path, String catalogName) {
        if (path == null) {
            return Optional.empty();
        }

        // If it's a file and ends with .cgproj, return it directly
        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".cgproj")) {
            return Optional.of(path);
        }

        if (Files.isDirectory(path)) {
            if (catalogName != null && !catalogName.isBlank()) {
                String expectedFile = catalogName.endsWith(".cgproj") ? catalogName : (catalogName + ".cgproj");
                Path direct = path.resolve(expectedFile);
                if (Files.isRegularFile(direct)) {
                    return Optional.of(direct);
                }
            }

            // Search the directory for a .cgproj file
            // glob == *.cgproj aka all files with .cgproj extension
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.cgproj")) {
                for (Path entry : stream) {
                    // return first instance of .cgproj file found in the directory
                    return Optional.of(entry);
                }
            } catch (IOException ignored) {
                logger.warn("Failed to read directory {}: {}", path, ignored);
            }
        }

        return Optional.empty();
    }

    /**
     * Validates the provided path to ensure it points to a valid .cgproj file or directory containing one.
     *
     * @param path The path to validate.
     * @return True if the path is valid, false otherwise.
     */
    private boolean validatePath(Path path) {
        // Validate if the file exists and is readable
        if (path == null || !Files.exists(path)) {
            return false;
        }

        // If it's a directory, check for a .cgproj file inside
        if (Files.isDirectory(path)) {
            Optional<Path> cgproj = resolveCgprojPath(path, null);
            return cgproj.isPresent() && Files.isRegularFile(cgproj.get()) && Files.isReadable(cgproj.get());
        }

        // If it's a file, check if it has the .cgproj extension and is readable
        return path.getFileName().toString().endsWith(".cgproj") &&
                Files.isRegularFile(path) &&
                Files.isReadable(path);
    }

    /**
     * Deletes a file or directory recursively.
     * If MAX_ROUND deletions are reached, the deletion will stop to prevent infinite loops.
     * Calls itself if a directory is found for each of the files in the directory.
     *
     * @param path  The path to delete.
     * @param round The current deletion round.
     * @return True if the deletion was successful, false otherwise.
     */
    private boolean deleteRecursively(Path path, int round) {
        round++;
        if (round >= MAX_DELETE_DEPTH)
            return false;
        try {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                    for (Path child : entries) {
                        deleteRecursively(child, round);
                    }
                }
            }
            Files.deleteIfExists(path);
            logger.debug("Deleting path: {} (round {})", path, round);
        } catch (IOException e) {
            logger.warn("Could not delete catalog: {}", path, e);
            return false;
        }

        return true;
    }

    /**
     * Retrieves the currently opened catalog, if any.
     *
     * @return An Optional containing the current Catalog, or empty if none is open.
     */
    public Optional<Catalog> getCurrentCatalog() {
        if (catalogService != null && CatalogDatabase.getInstance().isOpen()) {
            return Optional.of(catalogService.getCatalog());
        }
        return Optional.empty();
    }

    /**
     * Calculate the catalogs size based on the parent directory if it's possible
     * Otherwise calculate the sqlite database size
     *
     */
    private void calculateCatalogSize(Catalog catalog) {
        if (catalog == null || catalog.getCatalogPath() == null) {
            logger.warn("Cannot calculate catalog size: catalog or catalog path is null");
            return;
        }

        Path parentDirectory = Path.of(catalog.getCatalogPath());
        if (Files.isDirectory(parentDirectory)) {
            try (var paths = Files.walk(parentDirectory)) {
                long size = paths
                        .filter(p -> p.toFile().isFile())
                        .mapToLong(p -> p.toFile().length())
                        .sum();
                catalog.setSize(size);
                logger.debug("Calculated catalog size for '{}': {} bytes, started from: {}", catalog.getCatalogName(), size, parentDirectory);
            } catch (IOException ex) {
                logger.warn("Failed to calculate catalog size for '{}' at {}: {}", catalog.getCatalogName(), parentDirectory, ex.getMessage(), ex);
            }
        } else {
            catalog.setSize(Path.of(catalog.getCatalogPath()).toFile().length());
        }
    }

    /**
     * Retrieves the context of the currently opened catalog.
     *
     * @return The current CatalogContext.
     */
    public CatalogContext getCurrentCatalogContext() {
        return currentCatalogContext;
    }
}