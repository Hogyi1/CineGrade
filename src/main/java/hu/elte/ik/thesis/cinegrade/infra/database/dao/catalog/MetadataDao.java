// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;

import java.sql.*;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for the {@code photo_metadata} table of a catalog database.
 *
 * Each photo has at most one metadata row (keyed by {@code photo_id}) holding its camera and
 * capture information. The DAO does not own the {@link Connection}; the caller is responsible
 * for its lifecycle.
 */
public class MetadataDao {

    /**
     * Connection to the catalog database.
     */
    private final Connection connection;

    /**
     * Column list shared by the SELECT and the upsert statements.
     */
    private static final String COLUMNS =
            "photo_id, make, model, lens, iso, aperture, shutter_speed, focal_length, capture_date, color_style, raw_metadata_json";

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the catalog database
     */
    public MetadataDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Insert-or-update statement keyed on {@code photo_id}; its parameters are bound by {@link #bind}.
     */
    private static final String UPSERT_SQL =
            "INSERT INTO photo_metadata (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(photo_id) DO UPDATE SET " +
                    "make = excluded.make, model = excluded.model, lens = excluded.lens, " +
                    "iso = excluded.iso, aperture = excluded.aperture, shutter_speed = excluded.shutter_speed, " +
                    "focal_length = excluded.focal_length, capture_date = excluded.capture_date, " +
                    "color_style = excluded.color_style, raw_metadata_json = excluded.raw_metadata_json";

    /**
     * Number of rows added to a JDBC batch before it is flushed in {@link #upsertAll}.
     */
    private static final int BATCH_SIZE = 500;

    /**
     * Inserts the metadata, or updates it if the photo already has a metadata row.
     *
     * @param m metadata to store
     * @throws SQLException if the write fails
     */
    public void upsert(PhotoMetadata m) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, m);
            pstmt.executeUpdate();
        }
    }

    /**
     * Upserts many metadata rows in a single transaction. Statements are flushed in batches of
     * {@link #BATCH_SIZE}; on failure the transaction is rolled back and the previous auto-commit mode
     * is restored.
     *
     * @param items metadata to store; {@code null} or empty is a no-op
     * @throws SQLException if the write fails
     */
    public void upsertAll(List<PhotoMetadata> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            int count = 0;
            for (PhotoMetadata m : items) {
                bind(pstmt, m);
                pstmt.addBatch();
                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch(); // flush the remainder
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    /**
     * Binds metadata to the parameters of {@link #UPSERT_SQL}. A non-positive ISO or aperture value
     * is stored as SQL {@code NULL}.
     *
     * @param pstmt statement prepared from {@link #UPSERT_SQL}
     * @param m     metadata to write
     * @throws SQLException if a parameter cannot be bound
     */
    private static void bind(PreparedStatement pstmt, PhotoMetadata m) throws SQLException {
        pstmt.setString(1, m.getPhotoId());
        pstmt.setString(2, m.getMake());
        pstmt.setString(3, m.getModel());
        pstmt.setString(4, m.getLens());

        if (m.getIso() > 0) {
            pstmt.setInt(5, m.getIso());
        } else {
            pstmt.setNull(5, Types.INTEGER);
        }

        if (m.getAperture() > 0) {
            pstmt.setDouble(6, m.getAperture());
        } else {
            pstmt.setNull(6, Types.REAL);
        }

        pstmt.setString(7, m.getShutterSpeed());
        pstmt.setString(8, m.getFocalLength());
        pstmt.setString(9, m.getCaptureDate());
        pstmt.setString(10, m.getColorStyle());
        pstmt.setString(11, m.getRawMetadataJson());
    }

    /**
     * Finds the metadata of a photo.
     *
     * @param photoId id of the photo
     * @return the metadata, or empty if the photo has none
     * @throws SQLException if the query fails
     */
    public Optional<PhotoMetadata> findByPhotoId(String photoId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM photo_metadata WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMetadata(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes the metadata of a photo.
     *
     * @param photoId id of the photo
     * @return {@code true} if a row was deleted
     * @throws SQLException if the delete fails
     */
    public boolean delete(String photoId) throws SQLException {
        String sql = "DELETE FROM photo_metadata WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row of the result set to a {@link PhotoMetadata}. A SQL {@code NULL} ISO or
     * aperture becomes {@code null}.
     *
     * @param rs result set positioned on a row containing all {@link #COLUMNS}
     * @return the mapped metadata
     * @throws SQLException if a column cannot be read
     */
    private PhotoMetadata mapRowToMetadata(ResultSet rs) throws SQLException {
        int isoVal = rs.getInt("iso");
        Integer iso = rs.wasNull() ? null : isoVal;

        double apertureVal = rs.getDouble("aperture");
        Double aperture = rs.wasNull() ? null : apertureVal;

        return new PhotoMetadata(
                rs.getString("photo_id"),
                rs.getString("make"),
                rs.getString("model"),
                rs.getString("lens"),
                iso,
                aperture,
                rs.getString("shutter_speed"),
                rs.getString("focal_length"),
                rs.getString("capture_date"),
                rs.getString("color_style"),
                rs.getString("raw_metadata_json")
        );
    }
}