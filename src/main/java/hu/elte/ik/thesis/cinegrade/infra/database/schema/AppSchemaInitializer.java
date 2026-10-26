// [CONTAINS AI-GENERATED CODE: Table SQL strings]
package hu.elte.ik.thesis.cinegrade.infra.database.schema;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AppSchemaInitializer {

    private static final Logger logger = LogManager.getLogger(AppSchemaInitializer.class);

    private AppSchemaInitializer() {
    }

    private record SchemaEntity(String name, String sql) {
    }

    // [AI-GENERATED TABLE STRINGS]
    private static final String CREATE_TABLE_APP_SETTINGS = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT
            );
            """;

    private static final String CREATE_TABLE_RECENT_CATALOGS = """
            CREATE TABLE IF NOT EXISTS recent_catalogs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_directory TEXT NOT NULL,
                project_file_path TEXT NOT NULL,
                catalog_name TEXT NOT NULL,
                last_opened_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_TABLE_GLOBAL_PRESETS = """
            CREATE TABLE IF NOT EXISTS global_presets (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT DEFAULT 'User',
                edit_parameters_json TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_TABLE_USER_SESSION = """
            CREATE TABLE IF NOT EXISTS user_session (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                email TEXT NOT NULL,
                avatar_url TEXT,
                auth_token TEXT NOT NULL,
                last_login DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_INDEX_RECENT_CATALOGS_DATE = """
            CREATE INDEX IF NOT EXISTS idx_recent_catalogs_opened ON recent_catalogs(last_opened_at DESC);
            """;

    // A UNIQUE index instead of a column constraint, so migrated tables (ALTER TABLE ADD COLUMN can't add UNIQUE) get it too
    private static final String CREATE_INDEX_RECENT_CATALOGS_FILE = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_recent_catalogs_file ON recent_catalogs(project_file_path COLLATE NOCASE);
            """;

    private static final String CREATE_INDEX_GLOBAL_PRESETS_CAT = """
            CREATE INDEX IF NOT EXISTS idx_global_presets_category ON global_presets(category);
            """;

    private static final SchemaEntity[] TABLES = {
            new SchemaEntity("app_settings", CREATE_TABLE_APP_SETTINGS),
            new SchemaEntity("recent_catalogs", CREATE_TABLE_RECENT_CATALOGS),
            new SchemaEntity("global_presets", CREATE_TABLE_GLOBAL_PRESETS),
            new SchemaEntity("user_session", CREATE_TABLE_USER_SESSION)
    };

    private static final SchemaEntity[] INDEXES = {
            new SchemaEntity("idx_recent_catalogs_opened", CREATE_INDEX_RECENT_CATALOGS_DATE),
            new SchemaEntity("idx_recent_catalogs_file", CREATE_INDEX_RECENT_CATALOGS_FILE),
            new SchemaEntity("idx_global_presets_category", CREATE_INDEX_GLOBAL_PRESETS_CAT)
    };

    public static void initialize(Connection connection) {
        logger.info("Started app schema initialization");

        logger.info("Initializing app tables..");
        for (SchemaEntity table : TABLES) {
            logger.info("Creating app table: {}", table.name());
            try {
                executeStatement(connection, table.sql());
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.FAILED_TO_CREATE_TABLE, ex, table.name());
            }
            logger.info("App table has successfully been created: {}", table.name());
        }

        migrateRecentCatalogs(connection);

        logger.info("Initializing app indexes..");
        for (SchemaEntity index : INDEXES) {
            logger.info("Creating app index: {}", index.name());
            try {
                executeStatement(connection, index.sql());
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.FAILED_TO_CREATE_INDEX, ex, index.name());
            }
            logger.info("App index has successfully been created: {}", index.name());
        }

        logger.info("App schema initialization has successfully finished");
    }

    // [AI-GENERATED METHOD]
    // Old app databases stored only the project directory (catalog_path) and searched it for a .cgproj.
    // Rename it to project_directory and backfill project_file_path as <directory>/<name>.cgproj.
    private static void migrateRecentCatalogs(Connection connection) {
        try {
            if (columnExists(connection, "recent_catalogs", "catalog_path")) {
                logger.info("Migrating recent_catalogs.catalog_path -> project_directory");
                executeStatement(connection, "ALTER TABLE recent_catalogs RENAME COLUMN catalog_path TO project_directory;");
            }
            if (!columnExists(connection, "recent_catalogs", "project_file_path")) {
                logger.info("Migrating recent_catalogs: adding project_file_path");
                executeStatement(connection, "ALTER TABLE recent_catalogs ADD COLUMN project_file_path TEXT;");
                try (PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE recent_catalogs SET project_file_path = project_directory || ? || catalog_name || '.cgproj' WHERE project_file_path IS NULL;")) {
                    pstmt.setString(1, java.io.File.separator);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_SCHEMA_INIT_FAILED, ex, "recent_catalogs migration");
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (java.sql.Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void executeStatement(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
