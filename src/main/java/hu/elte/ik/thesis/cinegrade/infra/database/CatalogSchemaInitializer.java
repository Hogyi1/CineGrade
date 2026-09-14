package hu.elte.ik.thesis.cinegrade.infra.database;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CatalogSchemaInitializer {

    private static final Logger logger = LogManager.getLogger(CatalogSchemaInitializer.class);

    private CatalogSchemaInitializer() {
    }

    private record SchemaEntity(String name, String sql) {}

    private static final String CREATE_TABLE_CATALOG_INFO = """
            CREATE TABLE IF NOT EXISTS catalog_info (
                key TEXT PRIMARY KEY,
                value TEXT
            );
            """;

    private static final String CREATE_TABLE_CATALOGFOLDER = """
            CREATE TABLE IF NOT EXISTS catalog_folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                parent_id INTEGER,
                FOREIGN KEY (parent_id) REFERENCES catalog_folders(id) ON DELETE CASCADE
            );
            """;

    private static final String CREATE_TABLE_PHOTOENTRY = """
            CREATE TABLE IF NOT EXISTS photos (
                id TEXT PRIMARY KEY,
                folder_id INTEGER,
                file_path TEXT NOT NULL,
                file_name TEXT NOT NULL,
                file_size INTEGER,
                thumbnail_path TEXT,
                rating INTEGER DEFAULT 0,
                color_label TEXT,
                date_imported DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (folder_id) REFERENCES catalog_folders(id) ON DELETE SET NULL
            );
            """;

    private static final String CREATE_TABLE_PHOTO_METADATA = """
            CREATE TABLE IF NOT EXISTS photo_metadata (
                photo_id TEXT PRIMARY KEY,
                make TEXT,
                model TEXT,
                lens TEXT,
                iso INTEGER,
                aperture REAL,
                shutter_speed TEXT,
                focal_length TEXT,
                capture_date TEXT,
                color_style TEXT,
                raw_metadata_json TEXT,
                FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE
            );
            """;

    private static final String CREATE_TABLE_EDITSTATE = """
            CREATE TABLE IF NOT EXISTS edit_states (
                photo_id TEXT PRIMARY KEY,
                exposure REAL DEFAULT 0.0,
                contrast REAL DEFAULT 0.0,
                highlights REAL DEFAULT 0.0,
                shadows REAL DEFAULT 0.0,
                whites REAL DEFAULT 0.0,
                blacks REAL DEFAULT 0.0,
                temperature REAL DEFAULT 5500.0,
                tint REAL DEFAULT 0.0,
                saturation REAL DEFAULT 0.0,
                vibrance REAL DEFAULT 0.0,
                lut_path TEXT,
                curves_json TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE
            );
            """;

    private static final String CREATE_TABLE_EDIT_HISTORY = """
            CREATE TABLE IF NOT EXISTS edit_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                photo_id TEXT NOT NULL,
                step_order INTEGER NOT NULL,
                description TEXT NOT NULL,
                parameters_json TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE
            );
            """;

    private static final String CREATE_TABLE_PRESET = """
            CREATE TABLE IF NOT EXISTS presets (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT DEFAULT 'User',
                edit_parameters_json TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_INDEX_PHOTOPATH = "CREATE INDEX IF NOT EXISTS idx_photos_path ON photos(file_path);";
    private static final String CREATE_INDEX_RATING = "CREATE INDEX IF NOT EXISTS idx_photos_rating ON photos(rating);";
    private static final String CREATE_INDEX_FOLDER = "CREATE INDEX IF NOT EXISTS idx_photos_folder ON photos(folder_id);";
    private static final String CREATE_INDEX_METADATA_DATE = "CREATE INDEX IF NOT EXISTS idx_metadata_date ON photo_metadata(capture_date);";
    private static final String CREATE_INDEX_HISTORY_PHOTO = "CREATE INDEX IF NOT EXISTS idx_edit_history_photo ON edit_history(photo_id, step_order);";

    private static final SchemaEntity[] TABLES = {
            new SchemaEntity("catalog_info", CREATE_TABLE_CATALOG_INFO),
            new SchemaEntity("catalog_folders", CREATE_TABLE_CATALOGFOLDER),
            new SchemaEntity("photos", CREATE_TABLE_PHOTOENTRY),
            new SchemaEntity("photo_metadata", CREATE_TABLE_PHOTO_METADATA),
            new SchemaEntity("edit_states", CREATE_TABLE_EDITSTATE),
            new SchemaEntity("edit_history", CREATE_TABLE_EDIT_HISTORY),
            new SchemaEntity("presets", CREATE_TABLE_PRESET)
    };

    private static final SchemaEntity[] INDEXES = {
            new SchemaEntity("idx_photos_path", CREATE_INDEX_PHOTOPATH),
            new SchemaEntity("idx_photos_rating", CREATE_INDEX_RATING),
            new SchemaEntity("idx_photos_folder", CREATE_INDEX_FOLDER),
            new SchemaEntity("idx_metadata_date", CREATE_INDEX_METADATA_DATE),
            new SchemaEntity("idx_edit_history_photo", CREATE_INDEX_HISTORY_PHOTO)
    };

    public static void initialize(Connection connection) {
        logger.info("Started schema initialization");

        logger.info("Initializing tables..");
        for (SchemaEntity table : TABLES) {
            logger.info("Creating table: {}", table.name());
            try {
                executeStatement(connection, table.sql());
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.FAILED_TO_CREATE_TABLE, ex, table.name());
            }
            logger.info("Table has successfully been created: {}", table.name());
        }

        logger.info("Initializing indexes..");
        for (SchemaEntity index : INDEXES) {
            logger.info("Creating index: {}", index.name());
            try {
                executeStatement(connection, index.sql());
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.FAILED_TO_CREATE_INDEX, ex, index.name());
            }
            logger.info("Index has successfully been created: {}", index.name());
        }

        logger.info("Schema initialization has successfully finished");
    }

    private static void executeStatement(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
