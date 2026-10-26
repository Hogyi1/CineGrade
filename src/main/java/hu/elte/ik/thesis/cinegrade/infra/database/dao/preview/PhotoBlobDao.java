// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.preview;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoBlob;
import hu.elte.ik.thesis.cinegrade.domain.enums.BlobKind;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for the {@code photo_blobs} table of a catalog's preview database.
 *
 * Each row holds the binary image data (e.g. a thumbnail) of one {@link BlobKind} for one photo,
 * keyed by {@code (photo_id, kind)}. The preview database is a separate file from the catalog
 * database, so there is no foreign key to the photos table. The DAO does not own the
 * {@link Connection}; the caller is responsible for its lifecycle.
 */
public class PhotoBlobDao {

    /**
     * Column list shared by the SELECT and the upsert statements.
     */
    private static final String COLUMNS = "photo_id, kind, mime, width, height, data, created_at";
    /**
     * Insert-or-update statement keyed on {@code (photo_id, kind)}; its parameters are bound by {@link #bind}.
     * A rerender overwrites the row in place, and {@code created_at} is refreshed so it means "last rendered".
     */
    private static final String UPSERT_SQL =
            "INSERT INTO photo_blobs (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(photo_id, kind) DO UPDATE SET " +
                    "mime = excluded.mime, width = excluded.width, height = excluded.height, " +
                    "data = excluded.data, created_at = excluded.created_at";
    /**
     * Number of rows added to a JDBC batch before it is flushed in {@link #upsertAll}. Smaller than in the other DAOs because the rows carry image data.
     */
    private static final int BATCH_SIZE = 100; // rows carry image data, keep batches smaller than the other DAOs
    /**
     * Connection to the preview database.
     */
    private final Connection connection;

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the preview database
     */
    public PhotoBlobDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Binds a blob to the parameters of {@link #UPSERT_SQL}. A {@code null} width or height is stored
     * as SQL {@code NULL}.
     *
     * @param pstmt     statement prepared from {@link #UPSERT_SQL}
     * @param b         blob to write
     * @param createdAt ISO-8601 value for the {@code created_at} column
     * @throws SQLException if a parameter cannot be bound
     */
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

    /**
     * Parses a stored timestamp. Values written by this DAO are ISO-8601, while the
     * {@code CURRENT_TIMESTAMP} column default produces {@code "yyyy-MM-dd HH:mm:ss"} (UTC); both are accepted.
     *
     * @param value stored timestamp text, may be {@code null}
     * @return the parsed instant, or {@code null} if the value is {@code null} or unparseable
     */
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

    /**
     * Inserts the blob, or overwrites the existing row for the same photo and kind.
     * {@code created_at} is set to the current time.
     *
     * @param b blob to store
     * @throws SQLException if the write fails
     */
    public void upsert(PhotoBlob b) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, b, Instant.now().toString());
            pstmt.executeUpdate();
        }
    }

    /**
     * Upserts many blobs in a single transaction (e.g. the thumbnails of one import batch). All rows
     * share one {@code created_at} value. Statements are flushed in batches of {@link #BATCH_SIZE}; on
     * failure the transaction is rolled back and the previous auto-commit mode is restored.
     *
     * @param items blobs to store; {@code null} or empty is a no-op
     * @throws SQLException if the write fails
     */
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

    /**
     * Finds the blob of a photo for the given kind, including its image data.
     *
     * @param photoId id of the photo
     * @param kind    kind of blob to load
     * @return the blob, or empty if none is stored
     * @throws SQLException if the query fails
     */
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

    /**
     * Checks whether a blob is stored for the photo and kind, without reading the image data.
     *
     * @param photoId id of the photo
     * @param kind    kind of blob to look for
     * @return {@code true} if a row exists
     * @throws SQLException if the query fails
     */
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

    /**
     * Deletes the blob of a photo for the given kind.
     *
     * @param photoId id of the photo
     * @param kind    kind of blob to delete
     * @return {@code true} if a row was deleted
     * @throws SQLException if the delete fails
     */
    public boolean delete(String photoId, BlobKind kind) throws SQLException {
        String sql = "DELETE FROM photo_blobs WHERE photo_id = ? AND kind = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            pstmt.setString(2, kind.name());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Removes every kind of blob for the photo. Call it when the photo is deleted from the catalog,
     * since there is no foreign key across the database files.
     *
     * @param photoId id of the photo
     * @return the number of rows deleted
     * @throws SQLException if the delete fails
     */
    public int deleteAll(String photoId) throws SQLException {
        String sql = "DELETE FROM photo_blobs WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate();
        }
    }

    /**
     * Maps the current row of the result set to a {@link PhotoBlob}. A SQL {@code NULL} width or
     * height becomes {@code null}.
     *
     * @param rs result set positioned on a row containing all {@link #COLUMNS}
     * @return the mapped blob
     * @throws SQLException if a column cannot be read
     */
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
