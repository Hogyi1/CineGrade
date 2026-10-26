package hu.elte.ik.thesis.cinegrade.infra.services.storage.database;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog.CatalogSettingsDao;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class CatalogService {

    public static final String KEY_PROJECT_NAME = "project_name";
    public static final String KEY_PROJECT_DIRECTORY = "project_directory";
    public static final String KEY_PROJECT_FILE_PATH = "project_file_path";
    public static final String KEY_LAST_OPENED_AT = "last_opened_at";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_RECENT_CATALOG_ID = "recent_catalog_id";
    public static final String KEY_SCHEMA_VERSION = "schema_version";
    public static final String KEY_LAST_PANEL = "last_panel";

    private final CatalogSettingsDao settingsDao;
    @Getter
    private final Connection connection;

    public CatalogService(Connection connection) {
        this.connection = connection;
        this.settingsDao = new CatalogSettingsDao(connection);
    }

    /**
     * Writes the catalog's identity and current location into catalog_info.
     * Called on every open, so a moved project folder records where it lives now.
     * created_at is only written the first time.
     */
    // [AI-GENERATED METHOD]
    public void setMetaData(Catalog catalog) {
        if (catalog == null) {
            return;
        }

        try {
            if (catalog.getCatalogName() != null) {
                settingsDao.set(KEY_PROJECT_NAME, catalog.getCatalogName());
            }
            if (catalog.getProjectDirectory() != null) {
                settingsDao.set(KEY_PROJECT_DIRECTORY, catalog.getProjectDirectory().toString());
            }
            if (catalog.getProjectFilePath() != null) {
                settingsDao.set(KEY_PROJECT_FILE_PATH, catalog.getProjectFilePath().toString());
            }
            if (catalog.getLastOpenedAt() != null) {
                settingsDao.set(KEY_LAST_OPENED_AT, catalog.getLastOpenedAt().toString());
            } else {
                settingsDao.set(KEY_LAST_OPENED_AT, Instant.now().toString());
            }
            if (settingsDao.get(KEY_CREATED_AT) == null) {
                Instant createdAt = catalog.getCreatedAt() != null ? catalog.getCreatedAt() : Instant.now();
                settingsDao.set(KEY_CREATED_AT, createdAt.toString());
            }
            if (catalog.getId() > 0) {
                settingsDao.set(KEY_RECENT_CATALOG_ID, String.valueOf(catalog.getId()));
            }
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

    public void setProjectDirectory(String directory) {
        try {
            settingsDao.set(KEY_PROJECT_DIRECTORY, directory);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set project_directory");
        }
    }

    public void setProjectFilePath(String filePath) {
        try {
            settingsDao.set(KEY_PROJECT_FILE_PATH, filePath);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set project_file_path");
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

    public Optional<String> getProjectDirectory() {
        try {
            String val = settingsDao.get(KEY_PROJECT_DIRECTORY);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "project_directory");
        }
    }

    public Optional<String> getProjectFilePath() {
        try {
            String val = settingsDao.get(KEY_PROJECT_FILE_PATH);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(val);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "project_file_path");
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

    // [AI-GENERATED METHOD]
    /**
     * The workspace panel that was open when this catalog was last used.
     * Empty if never stored, or if the stored name is no longer a WorkspacePanel value.
     */
    public Optional<WorkspacePanel> getLastPanel() {
        try {
            String val = settingsDao.get(KEY_LAST_PANEL);
            return (val == null || val.isBlank()) ? Optional.empty() : Optional.of(WorkspacePanel.valueOf(val));
        } catch (SQLException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public void setLastPanel(WorkspacePanel panel) {
        try {
            settingsDao.set(KEY_LAST_PANEL, panel.name());
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, ex, "Failed to set last_panel");
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

    public Catalog getCatalog() {
        String name = getProjectName().orElse("Untitled");
        String directory = getProjectDirectory().orElse("");
        String filePath = getProjectFilePath().orElse("");
        Instant lastOpenedAt = getLastOpenedAt().orElse(Instant.now());
        Instant createdAt = getCreatedAt().orElse(Instant.now());

        int recentId = -1;
        try {
            Optional<String> idStr = getRecentCatalogId();
            if (idStr.isPresent()) {
                recentId = Integer.parseInt(idStr.get());
            }
        } catch (NumberFormatException ignored) {}

        return new Catalog(recentId, name, directory, filePath, lastOpenedAt, createdAt);
    }
}
