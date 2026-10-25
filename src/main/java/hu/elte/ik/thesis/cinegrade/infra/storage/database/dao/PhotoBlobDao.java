// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoBlob;
import hu.elte.ik.thesis.cinegrade.domain.enums.BlobKind;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class PhotoBlobDao {

    private static final String COLUMNS = "photo_id, kind, mime, width, height, data, created_at";
    // A rerender overwrites the row in place; created_at is refreshed so it means "last rendered"
    private static final String UPSERT_SQL =
            "INSERT INTO photo_blobs (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(photo_id, kind) DO UPDATE SET " +
                    "mime = excluded.mime, width = excluded.width, height = excluded.height, " +
                    "data = excluded.data, created_at = excluded.created_at";
    private static final int BATCH_SIZE = 100; // rows carry image data, keep batches smaller than the other DAOs
    private final Connection connection;

    public PhotoBlobDao(Connection connection) {
        this.connection = connection;
    }

    private static void bind(PreparedStatement pstmt, PhotoBlob b, String createdAt) throws SQLException {
        pstmt.setString(1, b.getPhotoId());
        pstmt.setString(2, b.getKind().name());
        pstmt.setString(3, b.getMime());

        if (b.getWidth() != null) {
            pstmt.setInt(4, b.getWidth());
        } else {
            pstmt.setNull(4, Types.INTEGER);
        }

        if (b.getHeight() != null) {
            pstmt.setInt(5, b.getHeight());
        } else {
            pstmt.setNull(5, Types.INTEGER);
        }

        pstmt.setBytes(6, b.getData());
        pstmt.setString(7, createdAt);
    }

    // Our writes are ISO-8601, but the CURRENT_TIMESTAMP default is "yyyy-MM-dd HH:mm:ss" (UTC).
    private static Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public void upsert(PhotoBlob b) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, b, Instant.now().toString());
            pstmt.executeUpdate();
        }
    }

    // Upserts many rows in a single transaction (e.g. thumbnails of one import batch)
    public void upsertAll(List<PhotoBlob> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        String now = Instant.now().toString();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            int count = 0;
            for (PhotoBlob b : items) {
                bind(pstmt, b, now);
                pstmt.addBatch();
                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public Optional<PhotoBlob> find(String photoId, BlobKind kind) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM photo_blobs WHERE photo_id = ? AND kind = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            pstmt.setString(2, kind.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPhotoBlob(rs));
                }
            }
        }
        return Optional.empty();
    }

    // Checks presence without reading the image data
    public boolean exists(String photoId, BlobKind kind) throws SQLException {
        String sql = "SELECT 1 FROM photo_blobs WHERE photo_id = ? AND kind = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            pstmt.setString(2, kind.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean delete(String photoId, BlobKind kind) throws SQLException {
        String sql = "DELETE FROM photo_blobs WHERE photo_id = ? AND kind = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            pstmt.setString(2, kind.name());
            return pstmt.executeUpdate() > 0;
        }
    }

    // Removes every kind for the photo; call it when the photo is deleted from the catalog (no FK across files)
    public int deleteAll(String photoId) throws SQLException {
        String sql = "DELETE FROM photo_blobs WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate();
        }
    }

    private PhotoBlob mapRowToPhotoBlob(ResultSet rs) throws SQLException {
        int widthVal = rs.getInt("width");
        Integer width = rs.wasNull() ? null : widthVal;

        int heightVal = rs.getInt("height");
        Integer height = rs.wasNull() ? null : heightVal;

        return PhotoBlob.builder()
                .photoId(rs.getString("photo_id"))
                .kind(BlobKind.valueOf(rs.getString("kind")))
                .mime(rs.getString("mime"))
                .width(width)
                .height(height)
                .data(rs.getBytes("data"))
                .createdAt(parseInstant(rs.getString("created_at")))
                .build();
    }
}
