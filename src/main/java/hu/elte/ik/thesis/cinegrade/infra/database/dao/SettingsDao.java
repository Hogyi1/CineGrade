package hu.elte.ik.thesis.cinegrade.infra.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDao {
    private final Connection connection;

    public SettingsDao(Connection connection) {
        this.connection = connection;
    }

    public String get(String key, String defaultValue) throws SQLException {
        String result = get(key);
        return (result == null || result.isBlank()) ? defaultValue : result;
    }

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
