package hu.elte.ik.thesis.cinegrade.app.managers.catalog;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.database.CatalogDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.CatalogSettingsDao;
import hu.elte.ik.thesis.cinegrade.infra.services.database.CatalogService;
import hu.elte.ik.thesis.cinegrade.infra.services.database.RecentCatalogService;
import hu.elte.ik.thesis.cinegrade.tasks.AppInitService;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class CatalogManager {

    private static final byte[] SQLITE_HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final Logger logger = LogManager.getLogger(CatalogManager.class);
    private final Path fallBackPath = Paths.get(AppConfig.INSTANCE.getProjectDirectory());
    private RecentCatalogService recentCatalogService;
    private CatalogService catalogService;
    private ObservableList<Catalog> catalogObserVableList;
    private static final int MAX_DELETE_DEPTH = 5;

    public CatalogManager() {
        recentCatalogService = new RecentCatalogService();
        refreshCatalogs();
    }

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

    public ObservableList<Catalog> getCatalogs() {
        return catalogObserVableList;
    }

    public void openCatalog(Catalog catalog) {
        if (catalog == null) {
            return;
        }
        Path path = Path.of(catalog.getCatalogPath());
        openProject(path, catalog.getCatalogName(), catalog.getId());
    }

    public void openCatalog(File file) {
        if (file == null) {
            throw new CineGradeException(ErrorCode.FILE_NOT_FOUND, "Selected file is null");
        }
        openCatalog(file.toPath());
    }

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
        Path cgprojFile = resolveCgprojPath(path, name);

        if (!validateProject(cgprojFile)) {
            throw new CineGradeException(ErrorCode.CATALOG_CORRUPTED, path.toAbsolutePath().toString());
        }

        try {
            CatalogDatabase.getInstance().openConnection(cgprojFile);
            catalogService = new CatalogService(CatalogDatabase.getInstance().getConnection());
            catalogService.setLastOpenedAt(Instant.now());

            Catalog catalog = getCurrentCatalog().orElse(null);
            Path dirPath = cgprojFile.getParent() != null ? cgprojFile.getParent() : cgprojFile;
            String normalizedDirPath = dirPath.toAbsolutePath().normalize().toString();

            Optional<Catalog> existingInRecents = catalogObserVableList.stream()
                    .filter(c -> (catalogId != null && c.getId() == catalogId) ||
                            (c.getCatalogPath() != null && Path.of(c.getCatalogPath()).toAbsolutePath().normalize().toString().equalsIgnoreCase(normalizedDirPath)))
                    .findFirst();

            if (existingInRecents.isPresent()) {
                Catalog existing = existingInRecents.get();
                recentCatalogService.updateLastOpenedAt(existing.getId(), Instant.now());
                catalogService.setSetting(CatalogService.KEY_RECENT_CATALOG_ID, String.valueOf(existing.getId()));
            } else {
                Catalog newRecent = recentCatalogService.addCatalog(name, dirPath);
                catalogService.setMetaData(newRecent);
            }

            logger.info("Catalog opened successfully: {}", cgprojFile);
        } catch (CineGradeException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CORRUPTED, ex, path.toString());
        }

        refreshCatalogs();
    }

    public void createNewProject(String name, Path path) {
        Path projectDirectory = path.resolve(name);

        if (Files.exists(projectDirectory.resolve(name + ".cgproj"))) {
            throw new CineGradeException(ErrorCode.CATALOG_ALREADY_EXISTS, projectDirectory.toString());
        }

        try {
            Files.createDirectories(projectDirectory);
            Files.createDirectories(projectDirectory.resolve("temp"));
            Files.createDirectories(projectDirectory.resolve("settings"));
        } catch (IOException ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, projectDirectory.toString());
        }

        Path dbFile = projectDirectory.resolve(name + ".cgproj");
        try {
            CatalogDatabase.getInstance().openConnection(dbFile);
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.CATALOG_CREATE_FAILED, ex, dbFile.toString());
        }

        Catalog catalog = recentCatalogService.addCatalog(name, projectDirectory);
        catalogService = new CatalogService(CatalogDatabase.getInstance().getConnection());
        catalogService.setMetaData(catalog);

        refreshCatalogs();
    }

    public Path getDefaultPath() {
        Optional<String> savedPath = recentCatalogService.getSavedPath();
        return savedPath.map(Path::of).filter(Files::isDirectory).orElse(fallBackPath);
    }

    private void refreshCatalogs() {
        Platform.runLater(() -> {
            List<Catalog> recentCatalogs = recentCatalogService.getRecentCatalogs();
            if (catalogObserVableList == null) {
                catalogObserVableList = FXCollections.observableList(recentCatalogs);
            } else {
                catalogObserVableList.setAll(recentCatalogs);
            }
        });
    }

    public boolean validateProject(Path path) {
        Path cgprojFile = resolveCgprojPath(path, null);
        if (!validatePath(cgprojFile)) {
            return false;
        }

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

    private Path resolveCgprojPath(Path path, String catalogName) {
        if (path == null) {
            return null;
        }
        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".cgproj")) {
            return path;
        }
        if (Files.isDirectory(path)) {
            if (catalogName != null && !catalogName.isBlank()) {
                String expectedFile = catalogName.endsWith(".cgproj") ? catalogName : (catalogName + ".cgproj");
                Path direct = path.resolve(expectedFile);
                if (Files.isRegularFile(direct)) {
                    return direct;
                }
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.cgproj")) {
                for (Path entry : stream) {
                    return entry;
                }
            } catch (IOException ignored) {
            }
        }
        return path;
    }

    private boolean validatePath(Path path) {
        if (path == null || !Files.exists(path)) {
            return false;
        }

        if (Files.isDirectory(path)) {
            Path cgproj = resolveCgprojPath(path, null);
            return cgproj != null && Files.isRegularFile(cgproj) && Files.isReadable(cgproj);
        }

        return path.getFileName().toString().endsWith(".cgproj") &&
                Files.isRegularFile(path) &&
                Files.isReadable(path);
    }

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
        } catch (IOException e) {
            logger.warn("Could not delete catalog: {}", path, e);
            return false;
        }

        return true;
    }

    public Optional<Catalog> getCurrentCatalog() {
        if (catalogService != null && CatalogDatabase.getInstance().isOpen()) {
            return Optional.of(catalogService.getCatalog());
        }
        return Optional.empty();
    }
}
