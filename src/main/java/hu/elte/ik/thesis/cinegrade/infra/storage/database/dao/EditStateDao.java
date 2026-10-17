// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.editing.EditState;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class EditStateDao {

    private static final String COLUMNS =
            "photo_id, exposure, contrast, highlights, shadows, whites, blacks, " +
                    "temperature, tint, saturation, vibrance, lut_path, curves_json, updated_at";
    private static final String UPSERT_SQL =
            "INSERT INTO edit_states (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(photo_id) DO UPDATE SET " +
                    "exposure = excluded.exposure, contrast = excluded.contrast, " +
                    "highlights = excluded.highlights, shadows = excluded.shadows, " +
                    "whites = excluded.whites, blacks = excluded.blacks, " +
                    "temperature = excluded.temperature, tint = excluded.tint, " +
                    "saturation = excluded.saturation, vibrance = excluded.vibrance, " +
                    "lut_path = excluded.lut_path, curves_json = excluded.curves_json, " +
                    "updated_at = excluded.updated_at";
    private static final int BATCH_SIZE = 500;
    private final Connection connection;

    public EditStateDao(Connection connection) {
        this.connection = connection;
    }

    private static void bind(PreparedStatement pstmt, EditState e, String updatedAt) throws SQLException {
        pstmt.setString(1, e.getPhotoId());
        pstmt.setDouble(2, e.getExposure());
        pstmt.setDouble(3, e.getContrast());
        pstmt.setDouble(4, e.getHighlights());
        pstmt.setDouble(5, e.getShadows());
        pstmt.setDouble(6, e.getWhites());
        pstmt.setDouble(7, e.getBlacks());
        pstmt.setDouble(8, e.getTemperature());
        pstmt.setDouble(9, e.getTint());
        pstmt.setDouble(10, e.getSaturation());
        pstmt.setDouble(11, e.getVibrance());
        pstmt.setString(12, e.getLutPath());
        pstmt.setString(13, e.getCurvesJson());
        pstmt.setString(14, updatedAt);
    }

    // Our writes are ISO-8601, but the CURRENT_TIMESTAMP default is "yyyy-MM-dd HH:mm:ss" (UTC).
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

    public void upsert(EditState e) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, e, Instant.now().toString());
            pstmt.executeUpdate();
        }
    }

    // Upserts many rows in a single transaction; all rows share one updated_at
    public void upsertAll(List<EditState> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        String now = Instant.now().toString();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            int count = 0;
            for (EditState e : items) {
                bind(pstmt, e, now);
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

    public Optional<EditState> findByPhotoId(String photoId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM edit_states WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToEditState(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean delete(String photoId) throws SQLException {
        String sql = "DELETE FROM edit_states WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private EditState mapRowToEditState(ResultSet rs) throws SQLException {
        return new EditState(
                rs.getString("photo_id"),
                rs.getDouble("exposure"),
                rs.getDouble("contrast"),
                rs.getDouble("highlights"),
                rs.getDouble("shadows"),
                rs.getDouble("whites"),
                rs.getDouble("blacks"),
                rs.getDouble("temperature"),
                rs.getDouble("tint"),
                rs.getDouble("saturation"),
                rs.getDouble("vibrance"),
                rs.getString("lut_path"),
                rs.getString("curves_json"),
                parseInstant(rs.getString("updated_at"))
        );
    }
}