// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class PhotoDao {

    private final Connection connection;
    private final Path catalogPath;

    private static final String COLUMNS =
            "id, original_path, import_method, import_id, file_path, file_name, file_size, thumbnail_path, rating, color_label, date_imported";

    public PhotoDao(Connection connection, Path catalogPath) {
        this.connection = connection;
        this.catalogPath = catalogPath;
    }

    public void insert(PhotoEntry photo) throws SQLException {
        batchInsert(List.of(photo));
    }

    public void batchInsert(List<PhotoEntry> photos) throws SQLException {
        if (photos == null || photos.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO photos (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

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
                pstmt.setString(8, photo.getDatabaseThumbnailPath(catalogPath));
                pstmt.setInt(9, photo.getRating());
                pstmt.setString(10, photo.getColorLabel());
                Instant imported = photo.getDateImported() != null ? photo.getDateImported() : Instant.now();
                pstmt.setString(11, imported.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

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

    public boolean updateRating(String id, int rating) throws SQLException {
        String sql = "UPDATE photos SET rating = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateColorLabel(String id, String colorLabel) throws SQLException {
        String sql = "UPDATE photos SET color_label = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, colorLabel);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateThumbnailPath(String id, String thumbnailPath) throws SQLException {
        String sql = "UPDATE photos SET thumbnail_path = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, thumbnailPath);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

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

    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM photos WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private PhotoEntry mapRowToPhoto(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String originalPath = rs.getString("original_path");
        String methodStr = rs.getString("import_method");
        ImportMethod importMethod = methodStr != null ? ImportMethod.valueOf(methodStr) : ImportMethod.ADD;
        String importId = rs.getString("import_id");
        String rawFilePath = rs.getString("file_path");
        long fileSize = rs.getLong("file_size");
        String rawThumbnailPath = rs.getString("thumbnail_path");
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
                id, importId, resolvedFile, rawThumbnailPath,
                fileSize, rating, colorLabel, dateImported,
                importMethod, originalPath
        );

        boolean photoExists = resolvedFile.exists();
        entry.setMissingPhoto(!photoExists);

        File resolvedThumb = entry.getResolvedThumbnailFile(catalogPath);
        boolean thumbExists = resolvedThumb != null && resolvedThumb.exists();
        entry.setMissingThumbnail(!thumbExists);

        return entry;
    }

    // Our inserts write ISO-8601 ("2026-10-12T10:15:30Z"), but the column default
    // CURRENT_TIMESTAMP writes "2026-10-12 10:15:30" (UTC). Handle both.
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