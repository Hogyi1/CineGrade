// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.storage.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class RecentCatalogDao {

    private static final String COLUMNS = "id, catalog_name, project_directory, project_file_path, last_opened_at, created_at";

    private final Connection connection;
    private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    public RecentCatalogDao(Connection connection) {
        this.connection = connection;
    }

    public void updateLastOpenedAt(int id, Instant now) throws SQLException {
        String sql = "UPDATE recent_catalogs SET last_opened_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(now));
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public Optional<Catalog> insert(String name, String projectDirectory, String projectFilePath) throws SQLException {
        String sql = "INSERT INTO recent_catalogs (catalog_name, project_directory, project_file_path) VALUES (?, ?, ?) " +
                "RETURNING " + COLUMNS;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, projectDirectory);
            pstmt.setString(3, projectFilePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCatalog(rs));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Catalog> findByProjectFilePath(String projectFilePath) throws SQLException {
        // Windows paths are case-insensitive
        String sql = "SELECT " + COLUMNS + " FROM recent_catalogs WHERE project_file_path = ? COLLATE NOCASE";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, projectFilePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCatalog(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Catalog> findAll() throws SQLException {
        List<Catalog> catalogs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM recent_catalogs ORDER BY last_opened_at";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                catalogs.add(mapRowToCatalog(rs));
            }
        }
        return catalogs;
    }

    private Catalog mapRowToCatalog(ResultSet rs) throws SQLException {

        Timestamp lastOpenedTs = rs.getTimestamp("last_opened_at", UTC_CALENDAR);
        Timestamp createdTs = rs.getTimestamp("created_at", UTC_CALENDAR);

        Instant lastOpenedAt = (lastOpenedTs != null) ? lastOpenedTs.toInstant() : null;
        Instant createdAt = (createdTs != null) ? createdTs.toInstant() : null;

        return new Catalog(
                rs.getInt("id"),
                rs.getString("catalog_name"),
                rs.getString("project_directory"),
                rs.getString("project_file_path"),
                lastOpenedAt,
                createdAt
        );
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM recent_catalogs WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}
