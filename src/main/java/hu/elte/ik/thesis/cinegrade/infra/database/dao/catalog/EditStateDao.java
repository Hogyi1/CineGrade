// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog;

import hu.elte.ik.thesis.cinegrade.domain.editing.EditState;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for the {@code edit_states} table of a catalog database.
 *
 * Each photo has at most one edit state row (keyed by {@code photo_id}) holding its develop
 * adjustments. Timestamps are written as ISO-8601 strings. The DAO does not own the
 * {@link Connection}; the caller is responsible for its lifecycle.
 */
public class EditStateDao {

    /**
     * Column list shared by the SELECT and the upsert statements.
     */
    private static final String COLUMNS =
            "photo_id, exposure, contrast, highlights, shadows, whites, blacks, " +
                    "temperature, tint, saturation, vibrance, lut_path, curves_json, updated_at";
    /**
     * Insert-or-update statement keyed on {@code photo_id}; its parameters are bound by {@link #bind}.
     */
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
    /**
     * Number of rows added to a JDBC batch before it is flushed in {@link #upsertAll}.
     */
    private static final int BATCH_SIZE = 500;
    /**
     * Connection to the catalog database.
     */
    private final Connection connection;

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the catalog database
     */
    public EditStateDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Binds an edit state to the parameters of {@link #UPSERT_SQL}.
     *
     * @param pstmt     statement prepared from {@link #UPSERT_SQL}
     * @param e         edit state to write
     * @param updatedAt ISO-8601 value for the {@code updated_at} column
     * @throws SQLException if a parameter cannot be bound
     */
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

    /**
     * Inserts the edit state, or updates it if the photo already has one. {@code updated_at} is set
     * to the current time.
     *
     * @param e edit state to store
     * @throws SQLException if the write fails
     */
    public void upsert(EditState e) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(UPSERT_SQL)) {
            bind(pstmt, e, Instant.now().toString());
            pstmt.executeUpdate();
        }
    }

    /**
     * Upserts many edit states. All rows share one {@code updated_at} value. Statements are flushed in
     * batches of {@link #BATCH_SIZE}. The DAO does not manage transactions: call it inside
     * {@code TransactionRunner.inTransaction/execute} so the batch commits or rolls back as one unit.
     *
     * @param items edit states to store; {@code null} or empty is a no-op
     * @throws SQLException if the write fails
     */
    public void upsertAll(List<EditState> items) throws SQLException {
        if (items == null || items.isEmpty()) return;

        String now = Instant.now().toString();
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
        }
    }

    /**
     * Finds the edit state of a photo.
     *
     * @param photoId id of the photo
     * @return the edit state, or empty if the photo has none
     * @throws SQLException if the query fails
     */
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

    /**
     * Deletes the edit state of a photo.
     *
     * @param photoId id of the photo
     * @return {@code true} if a row was deleted
     * @throws SQLException if the delete fails
     */
    public boolean delete(String photoId) throws SQLException {
        String sql = "DELETE FROM edit_states WHERE photo_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, photoId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Maps the current row of the result set to an {@link EditState}.
     *
     * @param rs result set positioned on a row containing all {@link #COLUMNS}
     * @return the mapped edit state
     * @throws SQLException if a column cannot be read
     */
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