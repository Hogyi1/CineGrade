// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.schema;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreviewSchemaInitializer {

    private static final Logger logger = LogManager.getLogger(PreviewSchemaInitializer.class);

    private PreviewSchemaInitializer() {
    }

    // No foreign key to photos(id): that table lives in the .cgproj file, SQLite can't enforce FKs across files.
    // Deleting a photo must delete its blobs explicitly (BlobStore.delete).
    private static final String CREATE_TABLE_PHOTO_BLOBS = """
            CREATE TABLE IF NOT EXISTS photo_blobs (
                photo_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                mime TEXT NOT NULL,
                width INTEGER,
                height INTEGER,
                data BLOB NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (photo_id, kind)
            );
            """;

    public static void initialize(Connection connection) {
        logger.info("Initializing preview schema..");
        try (PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_PHOTO_BLOBS)) {
            statement.execute();
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.FAILED_TO_CREATE_TABLE, ex, "photo_blobs");
        }
        logger.info("Preview schema initialization has successfully finished");
    }
}
