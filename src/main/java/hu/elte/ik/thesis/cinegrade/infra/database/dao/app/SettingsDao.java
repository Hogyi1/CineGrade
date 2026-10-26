// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access object for the {@code app_settings} key-value table of the application database.
 *
 * The DAO does not own the {@link Connection}; the caller is responsible for its lifecycle.
 */
public class SettingsDao {
    /**
     * Connection to the application database.
     */
    private final Connection connection;

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the application database
     */
    public SettingsDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Reads a setting, falling back to a default.
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
     * Reads a setting.
     *
     * @param key setting key
     * @return the stored value, or {@code null} if the key is not present
     * @throws SQLException if the query fails
     */
    public String get(String key) throws SQLException {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        }
        return null;
    }

    /**
     * Stores a setting, inserting it or overwriting the existing value (upsert).
     *
     * @param key   setting key
     * @param value value to store
     * @throws SQLException if the write fails
     */
    public void set(String key, String value) throws SQLException {
        String sql = """                                                                                     
                INSERT INTO app_settings (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value;
                """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }
}
