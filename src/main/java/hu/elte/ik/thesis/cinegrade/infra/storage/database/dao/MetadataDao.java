// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoMetadata;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class MetadataDao {

    private final Connection connection;

    private static final String COLUMNS =
            "photo_id, make, model, lens, iso, aperture, shutter_speed, focal_length, capture_date, color_style, raw_metadata_json";

    public MetadataDao(Connection connection) {
        this.connection = connection;
    }

    private static final String UPSERT_SQL =
            "INSERT INTO photo_metadata (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(photo_id) DO UPDATE SET " +
                    "make = excluded.make, model = excluded.model, lens = excluded.lens, " +
                    "iso = excluded.iso, aperture = excluded.aperture, shutter_speed = excluded.shutter_speed, " +
                    "focal_length = excluded.focal_length, capture_date = excluded.capture_date, " +
                    "color_style = excluded.color_style, raw_metadata_json = excluded.raw_metadata_json";

    private static final int BATCH_SIZE = 500;

    public void upsert(PhotoMetadata m) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, m);
            pstmt.executeUpdate();
        }
    }

    // Upserts many rows in a single transaction
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

    public boolean delete(String photoId) throws SQLException {
        String sql = "DELETE FROM photo_metadata WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate() > 0;
        }
    }

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