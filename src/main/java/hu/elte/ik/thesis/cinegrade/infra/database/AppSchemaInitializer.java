package hu.elte.ik.thesis.cinegrade.infra.database;

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

    private static final String CREATE_TABLE_APP_SETTINGS = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT
            );
            """;

    private static final String CREATE_TABLE_RECENT_CATALOGS = """
            CREATE TABLE IF NOT EXISTS recent_catalogs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                catalog_path TEXT NOT NULL UNIQUE,
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
                id INTEGER PRIMARY KEY,
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

    private static void executeStatement(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
