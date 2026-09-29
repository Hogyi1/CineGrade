package hu.elte.ik.thesis.cinegrade.infra.services.database;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.catalog.CatalogFolder;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.CatalogDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.CatalogSettingsDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.CatalogsFolderDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.UserDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class CatalogService {

    public static final String KEY_PROJECT_NAME = "project_name";
    public static final String KEY_PROJECT_PATH = "project_path";
    public static final String KEY_LAST_OPENED_AT = "last_opened_at";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_RECENT_CATALOG_ID = "recent_catalog_id";
    public static final String KEY_SCHEMA_VERSION = "schema_version";

    private final CatalogSettingsDao settingsDao;
    private final CatalogsFolderDao folderDao;
    private final Connection connection;

    public CatalogService(Connection connection) {
        this.connection = connection;
        this.settingsDao = new CatalogSettingsDao(connection);
        this.folderDao = new CatalogsFolderDao(connection);
    }

    public CatalogService() {
        this(CatalogDatabase.getInstance().getConnection());
    }

    public void setMetaData(Catalog catalog) {
        if (catalog == null) {
            return;
        }

        try {
            if (catalog.getCatalogName() != null) {
                settingsDao.set(KEY_PROJECT_NAME, catalog.getCatalogName());
            }
            if (catalog.getCatalogPath() != null) {
                settingsDao.set(KEY_PROJECT_PATH, catalog.getCatalogPath());
            }
            if (catalog.getLastOpenedAt() != null) {
                settingsDao.set(KEY_LAST_OPENED_AT, catalog.getLastOpenedAt().toString());
            } else {
                settingsDao.set(KEY_LAST_OPENED_AT, Instant.now().toString());
            }
            if (catalog.getCreatedAt() != null) {
                settingsDao.set(KEY_CREATED_AT, catalog.getCreatedAt().toString());
            } else {
                settingsDao.set(KEY_CREATED_AT, Instant.now().toString());
            }
            if (catalog.getId() > 0) {
                settingsDao.set(KEY_RECENT_CATALOG_ID, String.valueOf(catalog.getId()));
            }
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to save catalog metadata");
        }
    }

    public void setMetaData(String projectName, String projectPath, Instant lastOpenedAt, Instant createdAt) {
        try {
            if (projectName != null) {
                settingsDao.set(KEY_PROJECT_NAME, projectName);
            }
            if (projectPath != null) {
                settingsDao.set(KEY_PROJECT_PATH, projectPath);
            }
            settingsDao.set(KEY_LAST_OPENED_AT, (lastOpenedAt != null ? lastOpenedAt : Instant.now()).toString());
            settingsDao.set(KEY_CREATED_AT, (createdAt != null ? createdAt : Instant.now()).toString());
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to save catalog metadata");
        }
    }

    public void setLastOpenedAt(Instant now) {
        try {
            Instant timestamp = (now != null) ? now : Instant.now();
            settingsDao.set(KEY_LAST_OPENED_AT, timestamp.toString());
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set last_opened_at");
        }
    }

    public void setProjectPath(String path) {
        try {
            settingsDao.set(KEY_PROJECT_PATH, path);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set project_path");
        }
    }

    public void setProjectName(String name) {
        try {
            settingsDao.set(KEY_PROJECT_NAME, name);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set project_name");
        }
    }

    public Optional<String> getProjectName() {
        try {
            String val = settingsDao.get(KEY_PROJECT_NAME);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "project_name");
        }
    }

    public Optional<String> getRecentCatalogId() {
        try {
            String val = settingsDao.get(KEY_RECENT_CATALOG_ID);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "project_name");
        }
    }

    public Optional<String> getProjectPath() {
        try {
            String val = settingsDao.get(KEY_PROJECT_PATH);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "project_path");
        }
    }

    public Optional<Instant> getLastOpenedAt() {
        try {
            String val = settingsDao.get(KEY_LAST_OPENED_AT);
            if (val == null || val.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(val));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<Instant> getCreatedAt() {
        try {
            String val = settingsDao.get(KEY_CREATED_AT);
            if (val == null || val.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(val));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public String getSetting(String key, String defaultValue) {
        try {
            return settingsDao.get(key, defaultValue);
        } catch (SQLException ex) {
            return defaultValue;
        }
    }

    public void setSetting(String key, String value) {
        try {
            settingsDao.set(key, value);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set setting: " + key);
        }
    }

    public CatalogFolder createFolder(String name, Integer parentId) {
        try {
            return folderDao.insert(name, parentId);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.FOLDER_CREATE_FAILED, ex, name);
        }
    }

    public CatalogFolder createFolder(String name) {
        return createFolder(name, null);
    }

    public List<CatalogFolder> getAllFolders() {
        try {
            return folderDao.findAll();
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "Folders");
        }
    }

    public List<CatalogFolder> getRootFolders() {
        try {
            return folderDao.getRootFolders();
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "Root folders");
        }
    }

    public List<CatalogFolder> getSubFolders(int parentId) {
        try {
            return folderDao.findByParentId(parentId);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "Subfolders of " + parentId);
        }
    }

    public boolean deleteFolder(int id) {
        try {
            return folderDao.delete(id);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "deleteFolder: " + id);
        }
    }

    public boolean renameFolder(int id, String newName) {
        try {
            return folderDao.updateName(id, newName);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "renameFolder: " + id);
        }
    }

    public Catalog getCatalog() {
        String name = getProjectName().orElse("Untitled");
        String path = getProjectPath().orElse("");
        Instant lastOpenedAt = getLastOpenedAt().orElse(Instant.now());
        Instant createdAt = getCreatedAt().orElse(Instant.now());

        int recentId = -1;
        try {
            Optional<String> idStr = getRecentCatalogId();
            if (idStr.isPresent()) {
                recentId = Integer.parseInt(idStr.get());
            }
        } catch (NumberFormatException ignored) {}

        return new Catalog(recentId, name, path, lastOpenedAt, createdAt);
    }

    public Connection getConnection() {
        return connection;
    }
}
