// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.catalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access object for the {@code catalog_info} key-value table of a catalog database.
 *
 * Stores per-catalog settings and information. The DAO does not own the {@link Connection};
 * the caller is responsible for its lifecycle.
 */
public class CatalogSettingsDao {

    /**
     * Connection to the catalog database.
     */
    private final Connection connection;

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the catalog database
     */
    public CatalogSettingsDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Reads a catalog setting, falling back to a default.
     *
     * @param key          setting key
     * @param defaultValue value returned when the setting is missing, {@code null} or blank
     * @return the stored value, or {@code defaultValue}
     * @throws SQLException if the query fails
     */
    public String get(String key, String defaultValue) throws SQLException {
        String result = get(key);
        return (result == null || result.isBlank()) ? defaultValue : result;
    }

    /**
     * Reads a catalog setting.
     *
     * @param key setting key
     * @return the stored value, or {@code null} if the key is not present
     * @throws SQLException if the query fails
     */
    public String get(String key) throws SQLException {
        String sql = "SELECT value FROM catalog_info WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        }
        return null;
    }

    /**
     * Stores a catalog setting, inserting it or overwriting the existing value (upsert).
     *
     * @param key   setting key
     * @param value value to store
     * @throws SQLException if the write fails
     */
    public void set(String key, String value) throws SQLException {
        String sql = """                                                                                     
                INSERT INTO catalog_info (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value;
                """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }
}
