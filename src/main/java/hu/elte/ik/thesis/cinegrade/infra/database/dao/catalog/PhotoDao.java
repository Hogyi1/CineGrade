// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Data access object for the {@code photos} table of a catalog database.
 *
 * Stores one row per imported photo. File paths are converted between the form stored
 * in the database and absolute paths using the catalog directory. Thumbnails are not stored here
 * but in the preview database. The DAO does not own the
 * {@link Connection}; the caller is responsible for its lifecycle.
 */
public class PhotoDao {

    /**
     * Connection to the catalog database.
     */
    private final Connection connection;
    /**
     * Catalog directory used to resolve relative paths; may be {@code null}.
     */
    private final Path catalogPath;

    /**
     * Column list shared by the SELECT and INSERT statements.
     */
    private static final String COLUMNS =
            "id, original_path, import_method, import_id, file_path, file_name, file_size, mime_type, is_raw, rotation, rating, color_label, date_imported";

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection  open connection to the catalog database
     * @param catalogPath catalog directory, used to relativize stored paths and to resolve them on read;
     *                    if {@code null}, stored paths are used as-is
     */
    public PhotoDao(Connection connection, Path catalogPath) {
        this.connection = connection;
        this.catalogPath = catalogPath;
    }

    /**
     * Inserts a single photo.
     *
     * @param photo photo to insert
     * @throws SQLException if the insert fails
     * @see #batchInsert(List)
     */
    public void insert(PhotoEntry photo) throws SQLException {
        batchInsert(List.of(photo));
    }

    /**
     * Inserts many photos as one JDBC batch. The DAO does not manage transactions: call it inside
     * {@code TransactionRunner.inTransaction/execute} so the batch commits or rolls back as one unit.
     *
     * A missing import method defaults to {@link ImportMethod#ADD} and a missing import date to the
     * current time.
     *
     * @param photos photos to insert; {@code null} or empty is a no-op
     * @throws SQLException if the insert fails, e.g. on a duplicate id
     */
    public void batchInsert(List<PhotoEntry> photos) throws SQLException {
        if (photos == null || photos.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO photos (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (PhotoEntry photo : photos) {
                pstmt.setString(1, photo.getId());
                pstmt.setString(2, photo.getOriginalPath());
                pstmt.setString(3, photo.getImportMethod() != null ? photo.getImportMethod().name() : ImportMethod.ADD.name());

                if (photo.getImportId() != null) {
                    pstmt.setString(4, photo.getImportId());
                } else {
                    pstmt.setNull(4, Types.VARCHAR);
                }

                pstmt.setString(5, photo.getDatabaseFilePath(catalogPath));
                pstmt.setString(6, photo.getFileName());
                pstmt.setLong(7, photo.getFileSize());
                pstmt.setString(8, photo.getMimeType());
                pstmt.setInt(9, photo.isRaw() ? 1 : 0);
                pstmt.setInt(10, photo.getRotation());
                pstmt.setInt(11, photo.getRating());
                pstmt.setString(12, photo.getColorLabel());
                Instant imported = photo.getDateImported() != null ? photo.getDateImported() : Instant.now();
                pstmt.setString(13, imported.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    /**
     * Finds a photo by id.
     *
     * @param id id of the photo
     * @return the photo, or empty if there is no such photo
     * @throws SQLException if the query fails
     */
    public Optional<PhotoEntry> findById(String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM photos WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPhoto(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns every photo, ordered by import date (oldest first).
     *
     * @return all photos; empty if the catalog has none
     * @throws SQLException if the query fails
     */
    public List<PhotoEntry> findAll() throws SQLException {
        List<PhotoEntry> photos = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM photos ORDER BY date_imported ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                photos.add(mapRowToPhoto(rs));
            }
        }
        return photos;
    }

    /**
     * Sets the rating of a photo.
     *
     * @param id     id of the photo
     * @param rating the new rating
     * @return {@code true} if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateRating(String id, int rating) throws SQLException {
        String sql = "UPDATE photos SET rating = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Sets the color label of a photo.
     *
     * @param id         id of the photo
     * @param colorLabel the new color label, may be {@code null}
     * @return {@code true} if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateColorLabel(String id, String colorLabel) throws SQLException {
        String sql = "UPDATE photos SET color_label = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, colorLabel);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Sets the import the photo belongs to.
     *
     * @param id       id of the photo
     * @param importId id of the import, or {@code null} to clear it
     * @return {@code true} if a row was updated
     * @throws SQLException if the update fails
     */
    public boolean updateImportId(String id, String importId) throws SQLException {
        String sql = "UPDATE photos SET import_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (importId != null) {
                pstmt.setString(1, importId);
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a photo row. Files on disk are not touched.
     *
     * @param id id of the photo
     * @return {@code true} if a row was deleted
     * @throws SQLException if the delete fails
     */
    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM photos WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row of the result set to a {@link PhotoEntry}. Relative file paths are resolved
     * against the catalog directory, and the entry's missing-photo and missing-thumbnail flags are set
     * according to whether the files exist on disk.
     *
     * @param rs result set positioned on a row containing all {@link #COLUMNS}
     * @return the mapped photo
     * @throws SQLException if a column cannot be read
     */
    private PhotoEntry mapRowToPhoto(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String originalPath = rs.getString("original_path");
        String methodStr = rs.getString("import_method");
        ImportMethod importMethod = methodStr != null ? ImportMethod.valueOf(methodStr) : ImportMethod.ADD;
        String importId = rs.getString("import_id");
        String rawFilePath = rs.getString("file_path");
        long fileSize = rs.getLong("file_size");
        String mimeType = rs.getString("mime_type");
        boolean isRaw = rs.getInt("is_raw") != 0;
        int rotation = rs.getInt("rotation");
        int rating = rs.getInt("rating");
        String colorLabel = rs.getString("color_label");
        Instant dateImported = parseInstant(rs.getString("date_imported"));

        File resolvedFile;
        Path rawPath = Path.of(rawFilePath);
        if (rawPath.isAbsolute() || catalogPath == null) {
            resolvedFile = rawPath.toFile();
        } else {
            resolvedFile = catalogPath.resolve(rawPath).toFile();
        }

        PhotoEntry entry = new PhotoEntry(
                id, importId, resolvedFile,
                fileSize, rating, colorLabel, dateImported,
                importMethod, originalPath,
                mimeType, isRaw, rotation
        );

        // Thumbnails live in the preview database now; a missing one is regenerated by the ThumbnailService
        entry.setMissingPhoto(!resolvedFile.exists());

        return entry;
    }

    /**
     * Parses a stored timestamp. Values written by this DAO are ISO-8601, while the
     * {@code CURRENT_TIMESTAMP} column default produces {@code "yyyy-MM-dd HH:mm:ss"} (UTC); both are accepted.
     *
     * @param value stored timestamp text, may be {@code null}
     * @return the parsed instant, or the current time if the value is {@code null} or unparseable
     */
    private static Instant parseInstant(String value) {
        if (value == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}